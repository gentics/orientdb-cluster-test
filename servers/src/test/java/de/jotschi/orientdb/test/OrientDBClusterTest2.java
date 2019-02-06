package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

public class OrientDBClusterTest2 extends AbstractClusterTest {

	private final String NODE_NAME = "nodeB";

	@Before
	public void setup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data2"));
		initDB(NODE_NAME, "target/data2", "2481-2481", "2425-2425");
	}

	@Test
	public void testCluster() throws Exception {
		// Start the orient server - it will connect to other nodes and replicate the found database
		//startVertx();
		db.startOrientServer();

		// Replication may occur directly or we need to wait.
		db.waitForDB();

		loadCategoryId();
		loadProductIds();

		// Don't execute anything against this node
		System.in.read();
		sleep(5000);
		db.getServer().shutdown();

	}

}
