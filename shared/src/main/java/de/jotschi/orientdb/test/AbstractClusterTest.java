package de.jotschi.orientdb.test;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class AbstractClusterTest {

	protected Database db;

	public void start(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);

		// 1. Start the orient server
		Runnable t = () -> {
			try {
				db.startOrientServer();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		new Thread(t).start();

		// 2. Let the server startup
		System.out.println("Waiting");
		Thread.sleep(10000);
		System.out.println("Waited");

	}

	public void startVertx() {
		VertxOptions options = new VertxOptions();
		options.setClustered(true);
		Vertx.clusteredVertx(options, rh -> {
			System.out.println("Vertx Joined Cluster");
		});
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
}
