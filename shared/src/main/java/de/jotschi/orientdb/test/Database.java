package de.jotschi.orientdb.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
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
	private String httpPort;
	private String binPort;
	private LatchingDistributedLifecycleListener listener;

	public Database(String nodeName, String basePath, String httpPort, String binPort) {
		this.nodeName = nodeName;
		this.basePath = basePath;
		this.httpPort = httpPort;
		this.binPort = binPort;
		this.listener = new LatchingDistributedLifecycleListener(nodeName);
	}

	public OServer getServer() {
		return server;
	}

	private String getOrientServerConfig() throws IOException {
		InputStream configIns = getClass().getResourceAsStream("/config/orientdb-server-config.xml");
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		System.setProperty("PORT_CONFIG_HTTP", httpPort);
		System.setProperty("PORT_CONFIG_BIN", binPort);
		System.setProperty("ORIENTDB_PLUGIN_DIR", "orient-plugins");
		System.setProperty("plugin.directory", "plugins");
		System.setProperty("ORIENTDB_CONFDIR_NAME", "config");
		System.setProperty("ORIENTDB_NODE_NAME", nodeName);
		System.setProperty("ORIENTDB_DISTRIBUTED", "true");
		System.setProperty("ORIENTDB_DB_PATH", escapePath(basePath));
		configString = PropertyUtil.resolve(configString);
		return configString;
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

	public void addEdgeType(Supplier<OrientGraphNoTx> txProvider, String label, String superTypeName) {
		System.out.println("Adding edge type for label {" + label + "}");

		OrientGraphNoTx noTx = txProvider.get();
		try {
			OrientEdgeType edgeType = noTx.getEdgeType(label);
			if (edgeType == null) {
				String superClazz = "E";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				edgeType = noTx.createEdgeType(label, superClazz);

				// Add index
				String fieldKey = "name";
				edgeType.createProperty(fieldKey, OType.STRING);
				boolean unique = false;
				String indexName = label + "_name";
				edgeType.createIndex(indexName.toLowerCase(),
					unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString(),
					null, new ODocument().fields("ignoreNullValues", true), new String[] { fieldKey });
			}
		} finally {
			noTx.shutdown();
		}

	}

	public void addVertexType(Supplier<OrientGraphNoTx> txProvider, String typeName, String superTypeName) {

		System.out.println("Adding vertex type for class {" + typeName + "}");

		OrientGraphNoTx noTx = txProvider.get();
		try {
			OrientVertexType vertexType = noTx.getVertexType(typeName);
			if (vertexType == null) {
				String superClazz = "V";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				vertexType = noTx.createVertexType(typeName, superClazz);

				// Add index
				String fieldKey = "name";
				vertexType.createProperty(fieldKey, OType.STRING);
				boolean unique = false;
				String indexName = typeName + "_name";
				vertexType.createIndex(indexName.toLowerCase(),
					unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString(),
					null, new ODocument().fields("ignoreNullValues", true), new String[] { fieldKey });
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

	public OrientGraph getTx() {
		ODatabaseSession db = server.getContext().open("storage", "admin", "admin");
		return (OrientGraph) OrientGraphFactory.getTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	public OrientGraphNoTx getNoTx() {
		ODatabaseSession db = server.getContext().open("storage", "admin", "admin");
		return (OrientGraphNoTx) OrientGraphFactory.getNoTxGraphImplFactory().getGraph((ODatabaseDocumentInternal) db);
	}

	public void create(String name) {
		server.createDatabase(name, ODatabaseType.PLOCAL, OrientDBConfig.defaultConfig());
	}

}
