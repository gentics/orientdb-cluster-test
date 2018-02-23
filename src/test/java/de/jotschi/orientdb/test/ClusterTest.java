package de.jotschi.orientdb.test;

import org.junit.Test;

import de.jotschi.orientdb.test.docker.OrientDBServer;
import io.vertx.core.Vertx;

public class ClusterTest extends AbstractOrientDBTest {

	private static final String CLUSTERNAME = randomName();


	private static Vertx vertx = Vertx.vertx();

	private void startInitialServer() throws InterruptedException {
		OrientDBServer server = new OrientDBServer(vertx)
			.withInitCluster()
			.withClusterName(CLUSTERNAME)
			.withNodeName("master")
			.withClearFolders()
			.waitForStartup();

		server.start();
		server.awaitStartup(STARTUP_TIMEOUT);
	}

	@Test
	public void testCluster() throws InterruptedException {
		startInitialServer();
	}
}
