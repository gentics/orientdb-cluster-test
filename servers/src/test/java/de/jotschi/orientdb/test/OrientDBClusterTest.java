package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String NODE_NAME = "nodeA";

	@Before
	public void cleanup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data1"));
		OGlobalConfiguration.DISTRIBUTED_CONCURRENT_TX_MAX_AUTORETRY.setValue(1);
		initDB(NODE_NAME, "target/data1");
	}

	@Test
	public void testCluster() throws Exception {
		// Now start the OServer and provide the database to other nodes
		startVertx();
		db.startOrientServer();
		db.create("storage");

		// Setup needed types
		db.addVertexType(() -> db.getNoTx(), PRODUCT, null);
		db.addVertexType(() -> db.getNoTx(), CATEGORY, null);

		// Insert the needed vertices
		createCategory();
		insertProducts();

		// Now continue to update the products concurrently
		long timer1 = vertx.setPeriodic(50, this::productUpdater);
		long timer2 = vertx.setPeriodic(50, this::productUpdater);

		System.in.read();
		System.out.println("Stopping timers.");
		vertx.cancelTimer(timer1);
		vertx.cancelTimer(timer2);
		Thread.sleep(1000);
		System.out.println("Timer stopped.");
		System.out.println(
			"Press any key to update product one more time. This time no lock error should occure since the other TX's have been terminated.");

		System.in.read();
		productUpdater(null);

		System.in.read();
		sleep(5000);
		db.getServer().shutdown();

	}

	public void productUpdater(Long v) {
		try {
			tx(tx -> {
				OrientVertex product = getRandomProduct(tx);
				System.out.println("Update " + product.getId());
				product.setProperty("test", NODE_NAME + "@" + System.currentTimeMillis());
			});
		} catch (ONeedRetryException e) {
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
	}

}
