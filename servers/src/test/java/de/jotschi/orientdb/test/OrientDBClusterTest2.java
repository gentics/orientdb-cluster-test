package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.exception.OConcurrentCreateException;

public class OrientDBClusterTest2 extends AbstractClusterTest {

	private final String nodeName = "nodeB";
	private final String basePath = "target/data2/graphdb";

	@Before
	public void setup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data2"));
		initDB(nodeName, basePath);
	}

	@Test
	public void testCluster() throws Exception {
		// 1. Start the orient server - it will connect to other nodes and replicate the found database
		startVertx();
		db.startOrientServer();

		// 2. Replication may occur directly or we need to wait.
		db.waitForDB();

		// 3. The db has now been replicated. Lets open the db
		db.setupPool();

		vertx.eventBus().consumer("dummy", rh -> {
			System.out.println("Received message: " + rh.body());
		});

		// Lookup category
		Object categoryId = tx(tx -> {
			return tx.getVertices("@class", CATEGORY).iterator().next();
		});

		// 4. Insert some vertices
		long timer = vertx.setPeriodic(500, ph -> {
			try {
				tx(tx -> {
					addProduct(tx, categoryId);
					updateAllProducts(tx);
					tx.commit();
					updateAllProducts(tx);
					System.out.println("Count: " + tx.countVertices());
					sleep(1500);
				});
			} catch (OConcurrentCreateException e) {
				System.out.println("Ignoring OConcurrentCreateException - normally we would retry the action.");
			}
		});

		System.in.read();
		vertx.cancelTimer(timer);

	}

}
