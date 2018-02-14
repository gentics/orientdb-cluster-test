package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

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
		System.out.println("Started NodeA");
		db.setupPool();
		registerShutdown();

		handeActions("value2a");

		readStatus();
	}

}
