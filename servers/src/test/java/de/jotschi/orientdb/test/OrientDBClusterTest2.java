package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
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
		db.startOrientServer();
		startVertx();

		// 2. Replication may occur directly or we need to wait.
		db.waitForDB();

		// 3. Lookup category
		Object categoryId = tx(tx -> {
			Vertex v = tx.traversal().V().hasLabel(CATEGORY).next();
			return v.id();
		});

		// 4. Modify the graph
		long timer = vertx.setPeriodic(500, ph -> {
			try {
				tx(tx -> {
					Vertex category = tx.vertices(categoryId).next();
					updateRandomEdge(tx, category);
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

}
