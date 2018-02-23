package de.jotschi.orientdb.test;

import java.io.File;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.Vertx;

public class Server {

	private final String nodeName = "nodeA";
	private final String basePath = "target/data1/graphdb";
	protected Database db;

	private static final Vertx vertx = Vertx.vertx();

	// Environment variables
	public static final String INIT_CLUSTER = "INIT_CLUSTER";
	public static final String NODE_NAME_ENV = "NODE_NAME";
	public static final String CLUSTER_NAME_ENV = "CLUSTER_NAME";

	public static Vertx getVertx() {
		return vertx;
	}

	public static void main(String[] args) throws Exception {
		new Server().run();
	}

	public void run() throws Exception {

		initDB(nodeName, basePath);
		// Start the OServer and provide the database to other nodes
		db.startOrientServer();
		System.out.println("Started NodeA");
		db.setupPool();
		startCommandServer(db);
	}

	private void startCommandServer(Database db) {
		vertx.deployVerticle(new CRUDVerticle(db));
	}

	public void setup() throws Exception {
		// Check whether the db has already been replicated once
		boolean needDb = new File(basePath).exists();

		// 1. Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer();
		System.out.println("Started NodeB");

		// 2. Check whether we need to wait for other nodes in the cluster to provide the database
		if (needDb) {
			System.out.println("Waiting to join the cluster and receive the database.");
			db.waitForDB();
		}

		// 3. The DB has now been replicated. Lets open the DB
		db.setupPool();
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

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

}
