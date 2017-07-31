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

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class Database {

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

	private String escapePath(String path) {
		return StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(new File(path).getAbsolutePath()));
	}

	public OServer startOrientServer() throws Exception {
		String orientdbHome = new File("").getAbsolutePath();
		System.setProperty("ORIENTDB_HOME", orientdbHome);
		if (server == null) {
			this.server = OServerMain.create();
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

}
