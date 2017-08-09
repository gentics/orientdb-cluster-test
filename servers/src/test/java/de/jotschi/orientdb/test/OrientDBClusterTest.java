package de.jotschi.orientdb.test;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

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
		db.addVertexType("Test", null);

		// 3. Add the test vertex which we need later on
		OrientVertex root = addTestVertex();

		db.closePool();

		// 4. Now start the OServer and provide the database to other nodes
		db.startOrientServer();
		db.setupPool();

		// Now continue to update the test node
		int i = 0;
		while (true) {
			OrientGraph tx = db.getTx();
			try {
				String currentValue = "A" + i;
				root.reload();
				root.setProperty("name", currentValue);
				try {
					tx.commit();
					System.out.println("Updated test vertex..");
					root.reload();
					String nameAfterReload = root.getProperty("name");
					assertEquals(currentValue, nameAfterReload);
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

	private OrientVertex addTestVertex() {
		OrientVertex vertex;
		OrientGraph tx = db.getTx();
		try {
			vertex = tx.addVertex("class:Test");
			vertex.setProperty("name", "orientdb");
			tx.commit();
		} finally {
			tx.shutdown();
		}
		return vertex;
	}
}
