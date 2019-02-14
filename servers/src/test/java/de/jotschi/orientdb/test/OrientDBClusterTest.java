package de.jotschi.orientdb.test;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.tinkerpop.blueprints.Vertex;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String NODE_NAME = "nodeA";

	private static final long PRODUCT_COUNT = 100L;

	private static final long CATEGORY_COUNT = 5L;

	@Before
	public void setup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data1"));
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
		createCategories();
		insertProducts();

		// Now continue to update the products concurrently
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		System.out.println("Press any key to start load");
		System.in.read();
		executor.scheduleAtFixedRate(() -> productInserter(), 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		executor.scheduleAtFixedRate(() -> productInserter(), 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		executor.scheduleAtFixedRate(() -> productInserter(), 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Stopping threads.");
		executor.shutdown();
		Thread.sleep(1000);
		System.out.println("Timer stopped.");
		System.out.println(
			"Press any key to update product one more time. This time no lock error should occure since the other TX's have been terminated.");

		System.in.read();
		productInserter();

		System.in.read();
		sleep(5000);
		db.getServer().shutdown();

	}

	public void productInserter() {
		try {
			tx(tx -> {
				Vertex product = insertProduct(tx);
				product.setProperty("name", NODE_NAME + "@" + System.currentTimeMillis());
				System.out.println("Insert " + product.getId());
			});
			System.out.println("Inserted");
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
	}

	public void insertProducts() {
		tx(tx -> {
			for (int i = 0; i < PRODUCT_COUNT; i++) {
				insertProduct(tx);
			}
			return null;
		});
		System.out.println("Inserted " + PRODUCT_COUNT + " products..");
	}

	public void createCategories() {
		tx(tx -> {
			for (int i = 0; i < CATEGORY_COUNT; i++) {
				Object id = tx.addVertex("class:" + CATEGORY).getId();
				categoryIds.add(id);
				System.out.println("Create category " + id);
			}
			return null;
		});
		System.out.println("Created " + CATEGORY_COUNT + " categories...");
	}

}
