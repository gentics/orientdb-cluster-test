package de.jotschi.orientdb.test;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import com.orientechnologies.orient.core.Orient;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class Server {

	private static Logger log;

	static {
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(Server.class);
	}

	private String nodeName;
	private String clusterName;

	private final String basePath = "data";
	protected Database db;
	private boolean initGraph = false;

	private CountDownLatch latch = new CountDownLatch(1);
	private static final Vertx vertx = Vertx.vertx();

	// Environment variables
	public static final String INIT_CLUSTER = "INIT_CLUSTER";
	public static final String NODE_NAME_ENV = "NODE_NAME";
	public static final String CLUSTER_NAME_ENV = "CLUSTER_NAME";
	public static final String STARTUP_MSG = "SERVER_STARTED";

	public static Vertx getVertx() {
		return vertx;
	}

	public static void main(String[] args) throws Exception {
		boolean initGraph = System.getenv(INIT_CLUSTER) != null;
		String clusterName = System.getenv(CLUSTER_NAME_ENV);
		String nodeName = System.getenv(NODE_NAME_ENV);
		new Server(initGraph, clusterName, nodeName).run();
	}

	public Server(boolean initGraph, String clusterName, String nodeName) {
		this.initGraph = initGraph;
		this.clusterName = clusterName;
		this.nodeName = nodeName;
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				shutdown();
			} catch (Exception e) {
				log.error("Error while shutting down mesh.", e);
			}
		}));
	}

	private void shutdown() {
		log.info("Shutting down {" + getNodeName() + "}");
		if (db != null) {
			db.closePool();
			Orient.instance().shutdown();
			db.getServer().shutdown();
		}
		vertx.close();
	}

	public void run() throws Exception {
		registerShutdownHook();
		db = new Database(nodeName, basePath);
		db.init();

		if (initGraph) {
			log.info("Creating initial graph database");
			initGraph();
		}

		// Check whether the db has already been replicated once
		boolean needDb = new File(basePath).exists();

		db.startOrientServer();

		// Check whether we need to wait for other nodes in the cluster to provide the database
		if (needDb) {
			log.info("Waiting to join the cluster and receive the database.");
			db.waitForDB();
		}

		// Start the OServer and provide the database to other nodes
		db.setupPool();
		vertx.deployVerticle(new CRUDVerticle(this), rh -> {
			if (rh.failed()) {
				log.error("Starting CRUD server failed", rh.cause());
			} else {
				System.out.println(STARTUP_MSG);
			}
		});

		latch.await();
	}

	private Object createRootVertex() {
		OrientGraph tx = db.getTx();
		try {
			OrientVertex vertex = tx.addVertex("class:Root");
			vertex.setProperty("name", "version1");
			tx.commit();
			return vertex.getId();
		} finally {
			tx.shutdown();
		}
	}

	public void initGraph() {

		// 1. Setup the plocal database
		db.setupPool();

		// 2. Add need types to the database
		db.addVertexType("Root", null);
		db.addVertexType("Item", null);

		// 3. Add the test vertex which we need later on
		createRootVertex();

		db.closePool();
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getClusterName() {
		return clusterName;
	}

}
