package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class OrientDBClusterTest3 extends AbstractClusterTest {

	private final String NODE_NAME = "nodeC";

	@Before
	public void setup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data3"));
		initDB(NODE_NAME, "target/data3", "2482-2482", "2426-2426");
	}

	@Test
	public void testCluster() throws Exception {
		// Start the orient server - it will connect to other nodes and replicate the found database
		db.startOrientServer();

		// Replication may occur directly or we need to wait.
		db.waitForDB();

		// Don't execute anything against this node
		System.in.read();
		sleep(5000);
		db.getServer().shutdown();

	}

}
