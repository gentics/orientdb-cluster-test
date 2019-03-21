package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

public class OrientDBClusterTestNodeA extends AbstractClusterTest {

	private final String NODE_NAME = "nodeA";

	private static final long PRODUCT_COUNT = 100L;

	private static final long CATEGORY_COUNT = 5L;

	@Before
	public void setup() throws Exception {
		setup(NODE_NAME, "2480-2480", "2424-2424");
	}

	@Test
	public void testCluster() throws Exception {
		// Now start the OServer and provide the database to other nodes
		db.startOrientServer();
		db.create("storage");

		// Initially create the needed types and vertices
		setupDB();

		triggerLoad(() -> productInserter());

		System.in.read();
		sleep(5000);
		db.getServer().shutdown();

	}

	private void setupDB() {
		// Setup needed types
		db.addVertexType(() -> db.getNoTx(), PRODUCT, null);
		db.addVertexType(() -> db.getNoTx(), PRODUCT_INFO, null);
		db.addVertexType(() -> db.getNoTx(), CATEGORY, null);
		db.addEdgeType(() -> db.getNoTx(), "HAS_PRODUCT", null);
		db.addEdgeType(() -> db.getNoTx(), "HAS_INFO", null);

		// Insert the needed vertices
		createCategories();
		insertProducts();

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
