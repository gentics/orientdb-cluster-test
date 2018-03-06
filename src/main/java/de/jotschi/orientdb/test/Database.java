package de.jotschi.orientdb.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Database {

	private static final Logger log = LoggerFactory.getLogger(Database.class);

	private static final String ORIENTDB_DISTRIBUTED_CONFIG = "default-distributed-db-config.json";

	private static final String ORIENTDB_STUDIO_ZIP = "orientdb-studio-2.2.33.zip";
	
	private static final String ORIENTDB_HAZELCAST_CONFIG = "hazelcast.xml";

	private String nodeName;
	private String basePath;
	private OServer server;
	private LatchingDistributedLifecycleListener listener;
	private OrientGraphFactory factory;

	public Database(String nodeName, String basePath) {
		this.nodeName = nodeName;
		this.basePath = basePath;
		this.listener = new LatchingDistributedLifecycleListener(nodeName);
	}

	public void init() throws IOException {
		writeHazelcastConfig(new File("config", ORIENTDB_HAZELCAST_CONFIG));
		writeDistributedConfig(new File("config", ORIENTDB_DISTRIBUTED_CONFIG));
	}

	public OServer getServer() {
		return server;
	}

	private InputStream getOrientServerConfig() throws IOException {
		InputStream configIns = getClass().getResourceAsStream("/config/orientdb-server-config.xml");
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		configString = configString.replaceAll("%PLUGIN_DIRECTORY%", "orient-plugins");
		configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "finest");
		configString = configString.replaceAll("%FILE_LOG_LEVEL%", "fine");
		configString = configString.replaceAll("%DB_PATH%", "plocal:" + escapePath(basePath + "/storage"));
		configString = configString.replaceAll("%NODENAME%", nodeName);
		configString = configString.replaceAll("%DB_PARENT_PATH%", escapePath(basePath));
		InputStream stream = new ByteArrayInputStream(configString.getBytes(StandardCharsets.UTF_8));
		return stream;
	}

	private void writeHazelcastConfig(File hazelcastConfigFile) throws IOException {
		String resourcePath = "/config/" + ORIENTDB_HAZELCAST_CONFIG;
		InputStream configIns = getClass().getResourceAsStream(resourcePath);
		if (configIns == null) {
			log.error("Could not find default hazelcast configuration file {" + resourcePath + "} within classpath.");
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		FileUtils.writeStringToFile(hazelcastConfigFile, configString);
	}

	private void writeDistributedConfig(File distributedConfigFile) throws IOException {
		String resourcePath = "/config/" + ORIENTDB_DISTRIBUTED_CONFIG;
		InputStream configIns = getClass().getResourceAsStream(resourcePath);
		if (configIns == null) {
			log.error("Could not find default distributed configuration file {" + resourcePath + "} within classpath.");
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		FileUtils.writeStringToFile(distributedConfigFile, configString);
	}

	private String escapePath(String path) {
		return StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(new File(path).getAbsolutePath()));
	}
	
	
	/**
	 * Check the orientdb plugin directory and extract the orientdb studio plugin if needed.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void updateOrientDBPlugin() throws FileNotFoundException, IOException {
		InputStream ins = getClass().getResourceAsStream("/plugins/" + ORIENTDB_STUDIO_ZIP);
		File pluginDirectory = new File("orientdb-plugins");
		pluginDirectory.mkdirs();

		// Remove old plugins
		boolean currentPluginFound = false;
		for (File plugin : pluginDirectory.listFiles()) {
			if (plugin.isFile()) {
				String filename = plugin.getName();
				log.debug("Checking orientdb plugin: " + filename);
				if (filename.equals(ORIENTDB_STUDIO_ZIP)) {
					currentPluginFound = true;
					continue;
				}
				if (filename.startsWith("orientdb-studio-")) {
					plugin.delete();
				}
			}
		}

		if (!currentPluginFound) {
			log.info("Extracting OrientDB Studio");
			IOUtils.copy(ins, new FileOutputStream(new File(pluginDirectory, ORIENTDB_STUDIO_ZIP)));
		}

	}

	public OServer startOrientServer() throws Exception {
		log.info("Starting OrientDB server");
		String orientdbHome = new File("").getAbsolutePath();
		System.setProperty("ORIENTDB_HOME", orientdbHome);
		if (server == null) {
			this.server = OServerMain.create();
			updateOrientDBPlugin();
		}
		server.startup(getOrientServerConfig());
		OServerPluginManager manager = new OServerPluginManager();
		manager.config(server);
		server.activate();
		server.getDistributedManager().registerLifecycleListener(listener);
		manager.startup();
		postStartupDBEventHandling();
		return server;
	}

	public void addVertexType(String typeName, String superTypeName) {
		System.out.println("Adding vertex type for class {" + typeName + "}");
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			OrientVertexType vertexType = noTx.getVertexType(typeName);
			if (vertexType == null) {
				String superClazz = "V";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				vertexType = noTx.createVertexType(typeName, superClazz);
			}
		} finally {
			noTx.shutdown();
		}
	}

	private void postStartupDBEventHandling() {
		// Get the database status
		DB_STATUS status = server.getDistributedManager().getDatabaseStatus(nodeName, "storage");
		// Pass it along to the topology event bridge
		listener.onDatabaseChangeStatus(nodeName, "storage", status);
	}

	public void waitForDB() throws InterruptedException {
		System.out.println("Waiting for database");
		listener.waitForMainGraphDB(200, TimeUnit.SECONDS);
		System.out.println("Found database");
	}

	public void setupPool() {
		log.info("Creating graph factory for local storage");
		factory = new OrientGraphFactory("plocal:" + new File(basePath + "/storage").getAbsolutePath());
	}

	public OrientGraphNoTx getNoTx() {
		return factory.getNoTx();
	}

	public OrientGraph getTx() {
		return factory.getTx();
	}

	public void addEdgeType(String label) {
		System.out.println("Adding edge type for label {" + label + "}");
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			OrientEdgeType e = noTx.getEdgeType(label);
			if (e == null) {
				String superClazz = "E";
				e = noTx.createEdgeType(label, superClazz);
			}
		} finally {
			noTx.shutdown();
		}

	}

	public void closePool() {
		if (factory != null) {
			factory.close();
			factory = null;
		}
	}

}
