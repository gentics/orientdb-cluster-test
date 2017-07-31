package de.jotschi.orientdb.test;

import static com.tinkerpop.blueprints.Direction.OUT;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.orientechnologies.orient.enterprise.channel.binary.ODistributedRedirectException;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
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
		db.addVertexType("Product", null);
		db.addVertexType("Root", null);
		db.addEdgeType("HAS_ITEM");

		// 3. Add root vertex which we need later on
		Object rootId = addRootVertex();

		// 4. Now start the OServer and provide the database to other nodes
		db.startOrientServer();

		// Now continue to insert some nodes in the database
		int i = 0;
		while (true) {
			OrientGraph tx = db.getTx();
			try {
				OrientVertex root = loadRoot(rootId, tx);
				Vertex v = tx.addVertex("class:Product");
				v.setProperty("name", "A" + i);
				Edge e = root.addEdge("HAS_ITEM", v);
				e.setProperty("test", System.currentTimeMillis());
				System.out.println("Count: " + tx.countVertices());
				try {
					tx.commit();
					System.out.println("Count: " + tx.countVertices());
					assertSize(i + 1, root);
					i++;
				} catch (ODistributedRedirectException e1) {
					e1.printStackTrace();
				}

			} finally {
				tx.shutdown();
			}
			Thread.sleep(500);
		}
	}

	private void assertSize(int expectedSize, OrientVertex  root) {
		root.reload();
		Iterator<Edge> it = root.getEdges(OUT, "HAS_ITEM").iterator();
		int count = 0;
		while (it.hasNext()) {
			it.next();
			count++;
		}
		assertEquals("Did not find the expected amount of edges.", expectedSize, count);

	}

	private OrientVertex loadRoot(Object rootId, OrientGraph tx) {
		return tx.getVertex(rootId);
	}

	private Object addRootVertex() {
		Object rootId;
		OrientGraph tx = db.getTx();
		try {
			OrientVertex root = tx.addVertex("class:Root");
			rootId = root.getId();
			tx.commit();
		} finally {
			tx.shutdown();
		}
		return rootId;
	}
}
