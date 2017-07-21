package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.Orient;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String nodeName = "nodeA";
	private final String basePath = "target/data1/graphdb";

	@Before
	public void cleanup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data1"));
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {
		Orient.instance().startup();

		// 1. Setup the plocal database
		db.setupPool();

		// 2. Add a dummy type to the database
		db.addVertexType("Product", null);

		// 3. Now start the OServer and provide the database to other nodes
		db.startOrientServer();

		// Now continue to insert some nodes in the database
		while (true) {
			OrientGraph tx = db.getTx();
			try {
				Vertex v = tx.addVertex("Product");
				v.setProperty("name", "SOME VALUE");
				System.out.println("Count: " + db.getNoTx().countVertices());
				Thread.sleep(500);
				tx.commit();
			} finally {
				tx.shutdown();
			}
		}
	}
}
