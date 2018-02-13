package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class NodeATest extends AbstractClusterTest {

	private final String nodeName = "nodeA";
	private final String basePath = "target/data1/graphdb";

	@Before
	public void cleanup() throws Exception {
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {

		// Start the OServer and provide the database to other nodes
		db.startOrientServer();
		db.setupPool();

		// Continue to read the node
		while (true) {
			OrientGraph tx = db.getTx();
			try {
				OrientVertexType type = tx.getVertexType("Item0");
				OrientVertexType type2 = tx.getVertexType("Item0".toLowerCase());
				System.out.println("Count: " + tx.countVertices() + " type: " + type + " type2: " + type2);
			} finally {
				tx.shutdown();
			}
			Thread.sleep(1000);
		}

	}

}
