package de.jotschi.orientdb.test;

import org.junit.Test;

import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String nodeName = "nodeA";

	@Test
	public void testCluster() throws Exception {
		start(nodeName);

		db.setupFactory();
		OrientGraphNoTx graph = db.getFactory().getNoTx();

		for (int i = 0; i < 1000000; i++) {
			graph.addVertex(null);
			System.out.println("Count: " + graph.countVertices());
			Thread.sleep(1500);
		}
		System.in.read();
	}
}
