package de.jotschi.orientdb.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;

import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;

public class Database {

	private String nodeName;
	private String basePath;
	private OServer server;
	private LatchingDistributedLifecycleListener listener;
	private OrientGraphFactory factory;
	private OHazelcastPlugin hazelcastPlugin;

	public Database(String nodeName, String basePath) {
		this.nodeName = nodeName;
		this.basePath = basePath;
		this.listener = new LatchingDistributedLifecycleListener(nodeName);
	}

	public OServer getServer() {
		return server;
	}

	private InputStream getOrientServerConfig() throws IOException {
		try (InputStream configIns = getClass().getResourceAsStream("/config/orientdb-server-config.xml")) {
			StringWriter writer = new StringWriter();
			IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
			String configString = writer.toString();
			// configString = configString.replaceAll("%CONFDIR_NAME%","config");
			configString = configString.replaceAll("%PLUGIN_DIRECTORY%", "plugins");
			configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "finest");
			configString = configString.replaceAll("%FILE_LOG_LEVEL%", "fine");
			configString = configString.replaceAll("%DB_PATH%", "plocal:" + escapePath(basePath + "/storage"));
			configString = configString.replaceAll("%NODENAME%", nodeName);
			configString = configString.replaceAll("%DB_PARENT_PATH%", escapePath(basePath));
			InputStream stream = new ByteArrayInputStream(configString.getBytes(StandardCharsets.UTF_8));
			return stream;
		}
	}

	private String escapePath(String path) {
		return StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(new File(path).getAbsolutePath()));
	}

	public HazelcastInstance getHazelcast() {
		return hazelcastPlugin != null ? hazelcastPlugin.getHazelcastInstance() : null;
	}

	public OServer startOrientServer() throws Exception {
		String orientdbHome = new File("").getAbsolutePath();

		System.setProperty("ORIENTDB_HOME", orientdbHome);
		System.setProperty("CONF_DIR_NAME", "config");

		if (server == null) {
			try (InputStream ins = getOrientServerConfig()) {
				this.server = OServer.startFromStreamConfig(ins);
			}
		}

		//server.startup(getOrientServerConfig());
		//server.activate();
		ODistributedServerManager distributedManager = server.getDistributedManager();
		distributedManager.registerLifecycleListener(listener);

		if (server.getDistributedManager() instanceof OHazelcastPlugin) {
			hazelcastPlugin = (OHazelcastPlugin) distributedManager;
		}

		postStartupDBEventHandling();

		OrientDB context = server.getContext();

		// Open the db locally
		factory = new OrientGraphFactory(context, "storage", ODatabaseType.PLOCAL, "admin", "admin");
		return server;
	}

	public void addVertexType(String typeName, String superTypeName) {
		System.out.println("Adding vertex type for class {" + typeName + "}");
		OrientGraph noTx = factory.getNoTx();
		try {
			OSchema schema = getSchema(noTx);
			if (superTypeName == null) {
				superTypeName = "V";
			}
			schema.getOrCreateClass(typeName, schema.getOrCreateClass(superTypeName));
		} finally {
			noTx.close();
		}
	}

	public void waitForDB() throws InterruptedException {
		System.out.println("Waiting for database");
		listener.waitForMainGraphDB(200, TimeUnit.SECONDS);
		System.out.println("Found database");
	}

	public void setupPool() {
		factory = new OrientGraphFactory("plocal:" + new File(basePath + "/storage").getAbsolutePath());
	}

	public OrientGraph getNoTx() {
		return factory.getNoTx();
	}

	public OrientGraph getTx() {
		return factory.getTx();
	}

	/**
	 * Return the schema of the database.
	 * 
	 * @param noTx
	 * @return
	 */
	private OSchema getSchema(OrientGraph noTx) {
		OMetadata meta = noTx.getRawDatabase().getMetadata();
		OSchema schema = meta.getSchema();
		return schema;
	}

	private void postStartupDBEventHandling() {
		// Get the database status
		DB_STATUS status = server.getDistributedManager().getDatabaseStatus(nodeName, "storage");
		// Pass it along to the topology event bridge
		listener.onDatabaseChangeStatus(nodeName, "storage", status);
	}

	public void closePool() {
		factory.close();
		factory = null;
	}

}
