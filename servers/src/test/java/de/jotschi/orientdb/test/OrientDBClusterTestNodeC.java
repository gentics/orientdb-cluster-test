package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

import de.jotschi.orientdb.test.task.LoadTask;
import de.jotschi.orientdb.test.task.impl.ProductUpdater;

public class OrientDBClusterTestNodeC extends AbstractClusterTest {

	private final String NODE_NAME = "nodeC";

	@Override
	LoadTask getLoadTask() {
		return new ProductUpdater(this);
	}

	@Before
	public void setup() throws Exception {
		setup(NODE_NAME, "2482-2482", "2426-2426");
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
