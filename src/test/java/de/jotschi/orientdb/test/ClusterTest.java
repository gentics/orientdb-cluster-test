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

		// NodeA: Create new vertex
		nodeA.command(new JsonObject().put("command", "create").put("name", "V1"));

		// Add NodeB
		OrientDBServer nodeB = addNode(CLUSTERNAME, "nodeB", true);
		// Add NodeC
		OrientDBServer nodeC = addNode(CLUSTERNAME, "nodeC", true);

		nodeB.stop();
		Thread.sleep(14000);
		nodeC.stop();

		// NodeA: Create new vertex
		nodeA.command(new JsonObject().put("command", "create").put("name", "V2"));

		// Now start the stopped instance again
		Thread.sleep(2000);
		nodeC = addNode(CLUSTERNAME, "nodeC", false);
		nodeC.command(new JsonObject().put("command", "create").put("name", "V2"));

		Thread.sleep(2000);
		nodeB = addNode(CLUSTERNAME, "nodeB", false);
		nodeB.command(new JsonObject().put("command", "create").put("name", "V2"));

		Thread.sleep(1000);

		nodeA.command(new JsonObject().put("command", "read"));
		nodeB.command(new JsonObject().put("command", "read"));
		nodeC.command(new JsonObject().put("command", "read"));
	}
}
