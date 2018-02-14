package de.jotschi.orientdb.test;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class NodeBTest extends AbstractClusterTest {

	private final String nodeName = "nodeB";
	private final String basePath = "target/data2/graphdb";

	@Before
	public void cleanup() throws Exception {
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {
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
		handeActions("value2b");
	}

}
