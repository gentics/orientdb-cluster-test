package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

public class OrientDBClusterTestNodeD extends AbstractClusterTest {

	private final String NODE_NAME = "nodeD";

	@Before
	public void setup() throws Exception {
		setup(NODE_NAME, "2483-2483", "2427-2427");
	}

	@Test
	public void testCluster() throws Exception {
		// Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer();

		// Replication may occur directly or we need to wait.
		db.waitForDB();

		triggerLoad(getLoadTask());

		waitAndShutdown();

	}

}
