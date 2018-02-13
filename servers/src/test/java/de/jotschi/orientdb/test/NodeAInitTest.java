package de.jotschi.orientdb.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * Creates the local database which will be later used by node A
 */
public class NodeAInitTest extends AbstractClusterTest {

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

		// 2. Add need types to the database
		db.addVertexType("Root", null);
		for (int i = 0; i < 5; i++) {
			db.addVertexType("Item" + i, null);
		}

		// 3. Add the test vertex which we need later on
		Object id = createRootVertex();

		// Now continue to update the test node
		String vertexClass = null;

		OrientGraph tx = db.getTx();
		OrientVertex root = tx.getVertex(id);
		try {
			// Check the current vertex count (items+root vertex)
			assertEquals("Somehow the vertex of the last iteration was not persisted.", 1, tx.countVertices());

			// Check whether we need to choose a new vertex class
			if (vertexClass == null) {
				int rnd = (int) (Math.random() * 50);
				vertexClass = "class:Item" + rnd;
			}
			System.out.println("Adding item for class Item: " + vertexClass);
			OrientVertex item = tx.addVertex(vertexClass);
			root.addEdge("HAS_ITEM", item);
			tx.commit();
			System.out.println("Adding " + vertexClass + " was successful.");
			vertexClass = null;
		} catch (ONeedRetryException e) {
			System.out.println("\nNeed to retry for " + vertexClass);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			tx.shutdown();
		}
		db.closePool();
	}

	private Object createRootVertex() {
		OrientGraph tx = db.getTx();
		try {
			OrientVertex vertex = tx.addVertex("class:Root");
			vertex.setProperty("name", "orientdb");
			tx.commit();
			return vertex.getId();
		} finally {
			tx.shutdown();
		}
	}
}
