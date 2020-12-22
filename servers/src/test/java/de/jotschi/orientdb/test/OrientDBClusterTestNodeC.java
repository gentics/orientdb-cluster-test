package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

/**
 * @see OrientDBClusterTestNodeA
 */
public class OrientDBClusterTestNodeC extends AbstractClusterTest {

	private final String NODE_NAME = "nodeC";

	@Before
	public void setup() throws Exception {
		setup(NODE_NAME, "2482-2482", "2426-2426");
	}

	@Test
	public void testCluster() throws Exception {
		// Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer(true);

		waitAndShutdown();

	}

}
