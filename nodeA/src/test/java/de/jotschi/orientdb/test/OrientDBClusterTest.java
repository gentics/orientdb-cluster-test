package de.jotschi.orientdb.test;

import java.io.File;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String nodeName = "nodeA";

	@Test
	public void testCluster() throws Exception {
		start(nodeName);

		System.in.read();
		OrientGraphFactory factory = new OrientGraphFactory("plocal:" + new File("databases/db_testdb").getAbsolutePath());
		while(true) {
			Vertex v = factory.getNoTx().addVertex(null);
			v.setProperty("name", "SOME VALUE");
			System.out.println("Count: " + factory.getNoTx().countVertices());
			Thread.sleep(500);
		}
	}
}

