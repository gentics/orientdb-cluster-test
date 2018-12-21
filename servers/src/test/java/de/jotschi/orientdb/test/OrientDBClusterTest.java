package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.exception.OConcurrentCreateException;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String nodeName = "nodeA";
	private final String basePath = "target/data1/graphdb";

	@Before
	public void cleanup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data1"));
		OGlobalConfiguration.DISTRIBUTED_CONCURRENT_TX_MAX_AUTORETRY.setValue(1);
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {
		// 1. Setup DB once so that the OServer can later use it.
		setupLocalDB();

		// 3. Now start the OServer and provide the database to other nodes
		db.startOrientServer();
		startVertx();

		// Create category
		Object categoryId = tx(tx -> {
			return tx.addVertex(CATEGORY).id();
		});

		tx(tx -> {
			for (int i = 0; i < 1000; i++) {
				Vertex category = tx.vertices(categoryId).next();
				addProduct(tx, category);
			}
			return null;
		});

		// Now continue to insert some nodes in the database
		long timer = vertx.setPeriodic(500, ph -> {
			try {
				tx(tx -> {
					Vertex category = tx.vertices(categoryId).next();
					category.property("test", System.currentTimeMillis());
					System.out.println("Count: " + IteratorUtils.count(tx.vertices()));
				});
			} catch (OConcurrentCreateException e) {
				System.out.println("Ignoring OConcurrentCreateException - normally we would retry the action.");
			}
		});

		System.in.read();
		vertx.cancelTimer(timer);
		sleep(5000);
		db.getServer().shutdown();

	}

	private void setupLocalDB() {
		// Setup the plocal database
		db.setupPool();

		// Add a test types to the database
		db.addVertexType(PRODUCT, null);
		db.addVertexType(CATEGORY, null);

		// Finally close the plocal db
		db.closePool();
	}

}
