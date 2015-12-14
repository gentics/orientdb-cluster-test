package de.jotschi.orientdb.test;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String nodeName = "nodeA";

	@Test
	public void testCluster() throws Exception {
		start(nodeName);

		System.in.read();
		db.setupFactory();
		OrientGraphNoTx graph = db.getFactory().getNoTx();

		while(true) {
			Vertex v = graph.addVertex(null);
			v.setProperty("name", "SOME VALUE");
			System.out.println("Count: " + graph.countVertices());
			Thread.sleep(500);
			graph.commit();
		}
	}
}

