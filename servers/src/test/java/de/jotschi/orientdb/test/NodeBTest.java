package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class NodeBTest extends AbstractClusterTest {

	private final String nodeName = "nodeB";
	private final String basePath = "target/data2/graphdb";

	@Before
	public void cleanup() throws Exception {
		// FileUtils.deleteDirectory(new File("target/data2"));
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {
		// 1. Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer();

		// 2. Replication may occur directly or we need to wait.
		db.waitForDB();

		// 3. The DB has now been replicated. Lets open the DB
		db.setupPool();

		// 4. Insert some vertices
		while (true) {
			OrientGraph tx = db.getTx();
			try {
				OrientVertexType type = tx.getVertexType("Item0");
				System.out.println("Count: " + tx.countVertices() + " type: " + type);
				Thread.sleep(1500);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tx.shutdown();
			}
		}
	}

}
