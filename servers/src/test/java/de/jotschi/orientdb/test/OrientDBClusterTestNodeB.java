package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

import de.jotschi.orientdb.test.task.impl.ProductUpdaterTask;

public class OrientDBClusterTestNodeB extends AbstractClusterTest {

	private final String NODE_NAME = "nodeB";

	@Before
	public void setup() throws Exception {
		setup(NODE_NAME, "2481-2481", "2425-2425");
	}

	@Test
	public void testCluster() throws Exception {
		// Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer(true);
		triggerLoad(new ProductUpdaterTask(this));

		waitAndShutdown();

	}

}
