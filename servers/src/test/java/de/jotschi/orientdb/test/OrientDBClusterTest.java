package de.jotschi.orientdb.test;

import java.io.File;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientDBClusterTest extends AbstractClusterTest {

	private final String nodeName = "nodeA";
	private final String basePath = "target/data1/graphdb";

	@Test
	public void testCluster() throws Exception {
		OrientGraphFactory factory = new OrientGraphFactory("plocal:" + new File(basePath + "/storage").getAbsolutePath());
		// startESNode(nodeName);
		start(nodeName, basePath);
		startVertx();
		int i = 0;
		while (true) {
			vertx.eventBus().send("test", "someOtherValue");
			vertx.eventBus().publish("test", "SomeValue");

			OrientGraphNoTx graph = factory.getNoTx();
			try {
				Vertex v = graph.addVertex(null);
				v.setProperty("name", "SOME VALUE");
				System.out.println("Count: " + factory.getNoTx().countVertices());
				Thread.sleep(500);
			} finally {
				graph.shutdown();
			}
			i++;
		}
	}
}
