package de.jotschi.orientdb.test;

import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.Orient;

import de.jotschi.orientdb.test.task.impl.ProductUpdaterTask;

/**
 * This test starts a OrientDB server instance. After the test has been started you can trigger a write load by pressing enter up to 4 times. With each step
 * additional threads will be started to increase the write load. The {@link OrientDBClusterTestNodeB}, {@link OrientDBClusterTestNodeC} and
 * {@link OrientDBClusterTestBackupNode} tests can be used to launch additional cluster instances.
 * 
 * The goal of this test to reproduce OrientDB cluster instabilities which can occur pre 3.1.3 release when restarting node B, or C during an active load on
 * another node (e.g node A).
 */
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
		createInitialDB();

		// Now start the OServer and provide the database to other nodes
		db.startOrientServer(false);

		triggerLoad(new ProductUpdaterTask(this));

		waitAndShutdown();

	}

	// Initially create the needed types and vertices
	private void createInitialDB() {
		Orient.instance().startup();
		db.openLocally("storage");
		setupDB();
		db.close();
		Orient.instance().shutdown();
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
