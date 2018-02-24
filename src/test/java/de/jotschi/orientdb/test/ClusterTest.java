package de.jotschi.orientdb.test;

import org.junit.Test;

import de.jotschi.orientdb.test.docker.OrientDBServer;
import io.vertx.core.json.JsonObject;

public class ClusterTest extends AbstractOrientDBTest {

	private OrientDBServer startInitialServer() throws InterruptedException {
		OrientDBServer server = new OrientDBServer(vertx)
			.withInitCluster()
			.withClusterName(CLUSTERNAME)
			.withNodeName("master")
			.withClearFolders()
			.waitForStartup();

		server.start();
		server.awaitStartup(STARTUP_TIMEOUT);
		return server;
	}

	@Test
	public void testCluster() throws Exception {
		OrientDBServer nodeA = startInitialServer();
		nodeA.command(new JsonObject().put("command", "read"));
		nodeA.command(new JsonObject().put("command", "create").put("name", "V1"));

		OrientDBServer nodeB = addNode(CLUSTERNAME, "nodeB", true);
		nodeB.command(new JsonObject().put("command", "create").put("name", "V2"));
		nodeB.command(new JsonObject().put("command", "read"));

		OrientDBServer nodeC = addNode(CLUSTERNAME, "nodeC", true);
		nodeC.command(new JsonObject().put("command", "create").put("name", "V2"));
		nodeC.command(new JsonObject().put("command", "read"));

		// Stop both added nodes
		nodeB.stop();
		Thread.sleep(1000);
		nodeC.stop();
		Thread.sleep(1000);

		// Now start the nodes again
		nodeB = addNode(CLUSTERNAME, "nodeB", false);
		nodeB.command(new JsonObject().put("command", "read"));

		nodeC = addNode(CLUSTERNAME, "nodeC", false);
		nodeC.command(new JsonObject().put("command", "read"));

	}
}
