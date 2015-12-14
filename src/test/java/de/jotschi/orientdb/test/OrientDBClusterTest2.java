package de.jotschi.orientdb.test;

import org.junit.Test;

import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientDBClusterTest2 extends AbstractClusterTest {

	private final String nodeName = "nodeB";

	@Test
	public void testCluster() throws Exception {
		start(nodeName);
		System.in.read();
		db.setupFactory();
		OrientGraphNoTx graph = db.getFactory().getNoTx();
		for (int i = 0; i < 1000000; i++) {
			System.out.println("Count: " + graph.countVertices());
			Thread.sleep(1500);
		}
		System.in.read();

	}
}
