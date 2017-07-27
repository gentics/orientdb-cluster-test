package de.jotschi.orientdb.test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class AbstractClusterTest {

	protected Database db;

	protected Vertx vertx;

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

	public void startVertx() throws InterruptedException {
		VertxOptions options = new VertxOptions();
		options.setClustered(true);
		CountDownLatch latch = new CountDownLatch(1);
		Vertx.clusteredVertx(options, rh -> {
			System.out.println("Vertx Joined Cluster");
			vertx = rh.result();
			latch.countDown();
		});
		latch.await(10, TimeUnit.SECONDS);
	}

	public Node startESNode(String nodeName) {
		Settings settings = Settings.settingsBuilder()

				.put("threadpool.index.queue_size", -1)

				.put("http.enabled", false)

				.put("node.name", nodeName)

				.put("node.local", false)

				.put("http.cors.enabled", "true")

				.put("path.data", "essearch").build();
		NodeBuilder builder = NodeBuilder.nodeBuilder();
		// TODO configure ES cluster options
		System.out.println("Starting ES");
		Node node = builder.clusterName("testESCluster").settings(settings).node();
		// node.start();
		return node;
	}

	public void verifyConsistency(OrientGraph tx, String prefix) {

		Set<String> ids = new HashSet<>();
		for (Vertex v : tx.getVertices()) {
			String name = v.getProperty("name");
			if (name.startsWith(prefix)) {
				// System.out.println(name);
				ids.add(name);
			}
		}
		System.out.println("prefix: " + prefix + ": " + ids.size());
		for (int i = 0; i < ids.size(); i++) {
			String key = prefix + i;
			if (!ids.contains(key) && ids.contains(prefix + (i + 1))) {
				org.junit.Assert.fail("Found inconsistency. The entry for key {" + key + "} does not exist but the next does!");
			}
		}

	}
}
