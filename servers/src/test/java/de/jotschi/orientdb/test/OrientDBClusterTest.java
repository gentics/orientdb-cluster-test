package de.jotschi.orientdb.test;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String NODE_NAME = "nodeA";

	@Before
	public void cleanup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data1"));
		// OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(Integer.MAX_VALUE);
		initDB(NODE_NAME, "target/data1", "2480-2480", "2424-2424");
	}

	@Test
	public void testCluster() throws Exception {
		// Now start the OServer and provide the database to other nodes
		db.startOrientServer();
		db.create("storage");

		// Setup needed types
		db.addVertexType(() -> db.getNoTx(), PRODUCT, null);
		db.addVertexType(() -> db.getNoTx(), CATEGORY, null);

		// Insert the needed vertices
		createCategory();
		insertProducts();

		// Now continue to update the products concurrently
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		System.out.println("Press any key to start load");
		System.in.read();
		executor.scheduleAtFixedRate(() -> productUpdater(), 100, 50, TimeUnit.MILLISECONDS);
		System.in.read();
		executor.scheduleAtFixedRate(() -> productUpdater(), 100, 50, TimeUnit.MILLISECONDS);
		System.in.read();
		executor.scheduleAtFixedRate(() -> productUpdater(), 100, 50, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Stopping threads.");
		executor.shutdown();
		Thread.sleep(1000);
		System.out.println("Timer stopped.");
		System.out.println(
			"Press any key to update product one more time. This time no lock error should occure since the other TX's have been terminated.");

		System.in.read();
		productUpdater();

		System.in.read();
		sleep(5000);
		db.getServer().shutdown();

	}

	public void productUpdater() {
		try {
			tx(tx -> {
				OrientVertex category = getCategory(tx);
				addProduct(tx, category);
				OrientVertex product = getRandomProduct(tx);
				System.out.println("Update " + product.getId());
				product.setProperty("test", NODE_NAME + "@" + System.currentTimeMillis());
			});
			System.out.println("Updated");
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
	}

}
