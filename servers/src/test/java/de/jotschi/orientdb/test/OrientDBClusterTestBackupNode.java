package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

import de.jotschi.orientdb.test.task.impl.BackupTask;


/**
 * @see OrientDBClusterTestNodeA
 */
public class OrientDBClusterTestBackupNode extends AbstractClusterTest {

	private final String NODE_NAME = "node-backup";

	@Before
	public void setup() throws Exception {
		setup(NODE_NAME, "2483-2483", "2427-2427");
	}

	@Test
	public void testCluster() throws Exception {
		// Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer(true);

		triggerSlowLoad(new BackupTask(this));

		waitAndShutdown();

	}

}
