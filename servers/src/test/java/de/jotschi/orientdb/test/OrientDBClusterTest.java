package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.exception.OConcurrentCreateException;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String nodeName = "nodeA";
	private final String basePath = "target/data1/graphdb";

	@Before
	public void cleanup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data1"));
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {
		// 1. Setup the plocal database
		db.setupPool();

		// 2. Add a test types to the database
		db.addVertexType(PRODUCT, null);
		db.addVertexType(CATEGORY, null);

		// 3. Now start the OServer and provide the database to other nodes
		startVertx();
		db.startOrientServer();

		// Create category
		Object categoryId = tx(tx -> {
			return tx.addVertex("class:" + CATEGORY).getId();
		});

		// Now continue to insert some nodes in the database
		long timer = vertx.setPeriodic(500, ph -> {
			try {
				tx(tx -> {
					vertx.eventBus().publish("dummy", "hello world");
					addProduct(tx, categoryId);
					updateAllProducts(tx);
					tx.commit();
					updateAllProducts(tx);
					System.out.println("Count: " + tx.countVertices());
					sleep(500);
				});
			} catch (OConcurrentCreateException e) {
				System.out.println("Ignoring OConcurrentCreateException - normally we would retry the action.");
			}
		});

		System.in.read();
		vertx.cancelTimer(timer);

	}

}
