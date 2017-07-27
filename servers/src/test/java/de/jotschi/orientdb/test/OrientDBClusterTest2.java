package de.jotschi.orientdb.test;

import java.io.File;


import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class OrientDBClusterTest2 extends AbstractClusterTest {

	private final String nodeName = "nodeB";
	private final String basePath = "target/data2/graphdb";

	@Before
	public void setup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data2"));
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {
		// 1. Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer();

		// 2. Replication may occur directly or we need to wait.
		db.waitForDB();

		// 3. The db has now been replicated. Lets open the db
		db.setupPool();

		// 4. Insert some vertices
		int i = 0;
		while (true) {
			OrientGraph tx = db.getTx();
			try {
				Vertex v = db.getNoTx().addVertex("class:Product");
				v.setProperty("name", "B" + i);
				System.out.println("Count: " + tx.countVertices());
				Thread.sleep(1500);
				verifyConsistency(tx, "A");
				verifyConsistency(tx, "B");
				tx.commit();
			} finally {
				tx.shutdown();
			}
			i++;
		}

	}

	
}
