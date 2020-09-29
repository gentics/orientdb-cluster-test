package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

import de.jotschi.orientdb.test.task.impl.ProductUpdaterTask;

public class OrientDBClusterLoadTest extends AbstractClusterTest {

	private static final long PRODUCT_COUNT = 100L;

	private static final long CATEGORY_COUNT = 5L;

	@Before
	public void setup() throws Exception {
		setup("localhost", "root", "finger");
		db.create("storage");
	}

	@Test
	public void testCluster() throws Exception {
		createInitialDB();

		db.openRemotely("storage");
		triggerLoad(new ProductUpdaterTask(this));
		waitAndShutdown();
		db.close();

	}

	// Initially create the needed types and vertices
	private void createInitialDB() {
		db.openRemotely("storage");
		setupDB();
		db.close();
	}

	// Setup needed types
	private void setupDB() {

		// 1. Add base type with uuid unique index
		db.addVertexType(() -> db.getNoTx(), BASE, null, uuidTypeModifier());

		// 2. Add extra types with .name index
		db.addVertexType(() -> db.getNoTx(), PRODUCT, BASE, nameTypeModifier());
		db.addVertexType(() -> db.getNoTx(), PRODUCT_INFO, BASE, nameTypeModifier());
		db.addVertexType(() -> db.getNoTx(), CATEGORY, BASE, nameTypeModifier());

		// 3. Don't use HAS_PRODUCT in this testcase. We don't use supernodes anymore.
		// db.addEdgeType(() -> db.getNoTx(), HAS_PRODUCT, null);
		db.addEdgeType(() -> db.getNoTx(), HAS_INFO, null);

		// Insert the graph to test with
		createCategories(CATEGORY_COUNT);
		insertProducts(PRODUCT_COUNT);

	}

}
