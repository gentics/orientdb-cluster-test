package de.jotschi.orientdb.test;

import java.io.File;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

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
		db.addVertexType("Product", null);
		db.addVertexType("Category", null);

		// 3. Now start the OServer and provide the database to other nodes
		startVertx();
		db.startOrientServer();

		Object categoryId = tx(tx -> {
			return tx.addVertex("class:Category").getId();
		});

		// Now continue to insert some nodes in the database
		while (true) {
			tx(tx -> {
				OrientVertex category = tx.getVertex(categoryId);
				vertx.eventBus().publish("dummy", "hello world");
				Vertex v = tx.addVertex("class:Product");
				v.setProperty("name", "SOME VALUE");
				category.addEdge("TEST", v);

				System.out.println("Count: " + db.getNoTx().countVertices());
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			});
		}
	}

	public <T> T tx(Function<OrientBaseGraph, T> handler) {
		OrientGraph tx = db.getTx();
		try {
			T result = handler.apply(tx);
			tx.commit();
			return result;
		} finally {
			tx.shutdown();
		}
	}
}
