package de.jotschi.orientdb.test;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

/**
 * Creates the local database which will be later used by node A
 */
public class NodeAInitTest extends AbstractClusterTest {

	private final String basePath = "target/data1/graphdb";

	@Before
	public void cleanup() throws Exception {
		FileUtils.deleteDirectory(new File("target/data1"));
		FileUtils.deleteDirectory(new File("target/data2"));
		initDB(null, basePath);
	}

	@Test
	public void testCluster() throws Exception {

		// 1. Setup the plocal database
		db.setupPool();

		// 2. Add need types to the database
		db.addVertexType("Root", null);

		// 3. Add the test vertex which we need later on
		createRootVertex();

		db.closePool();
	}

	private Object createRootVertex() {
		OrientGraph tx = db.getTx();
		try {
			OrientVertex vertex = tx.addVertex("class:Root");
			vertex.setProperty("name", "version1");
			tx.commit();
			return vertex.getId();
		} finally {
			tx.shutdown();
		}
	}
}
