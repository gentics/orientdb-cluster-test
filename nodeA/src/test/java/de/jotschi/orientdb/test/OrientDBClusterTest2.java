package de.jotschi.orientdb.test;

import java.io.File;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientDBClusterTest2 extends AbstractClusterTest {

	private final String nodeName = "nodeB";
	private final String dbPath = "target/data2/graphdb";

	@Test
	public void testCluster() throws Exception {
		// startESNode();
		start(nodeName, dbPath);
		// startVertx();
		System.out.println("READY");
		System.in.read();
		OrientGraphFactory factory = new OrientGraphFactory("plocal:" + new File(dbPath).getAbsolutePath());
		while (true) {
			OrientGraphNoTx graph = factory.getNoTx();
			try {
				Vertex v = factory.getNoTx().addVertex(null);
				v.setProperty("name", "SOME VALUE");
				System.out.println("Count: " + graph.countVertices());
				Thread.sleep(1500);
			} finally {
				graph.shutdown();
			}
		}

	}
}
