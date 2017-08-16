package de.jotschi.orientdb.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.enterprise.channel.binary.ODistributedRedirectException;
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

		// 2. Add need types to the database
		db.addVertexType("Root", null);
		for (int i = 0; i < 50; i++) {
			db.addVertexType("Item" + i, null);
		}

		// 3. Add the test vertex which we need later on
		Object id = createRootVertex();

		db.closePool();

		// 4. Now start the OServer and provide the database to other nodes
		db.startOrientServer();
		db.setupPool();

		// Now continue to update the test node
		int n = 0;
		while (true) {
			OrientGraph tx = db.getTx();
			OrientVertex root = tx.getVertex(id);
			try {
				// Check the current vertex count (items+root vertex)
				assertEquals("Somehow the vertex of the last iteration was not persisted.", n + 1, tx.countVertices());

				int rnd = (int) (Math.random() * 50);
				System.out.println("Adding item for class Item" + rnd);
				OrientVertex item = tx.addVertex("class:Item" + rnd);
				root.addEdge("HAS_ITEM", item);
				n++;
				tx.commit();
			} catch (ODistributedRedirectException e) {
				// Ignoring exception as suggested by #8978
			} catch (OConcurrentModificationException e) {
				n--;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				tx.shutdown();
			}
			Thread.sleep(500);
		}

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
