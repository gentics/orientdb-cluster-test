package de.jotschi.orientdb.test;

import de.jotschi.orientdb.test.docker.OrientDBServer;
import io.vertx.core.Vertx;

public class AbstractOrientDBTest {

	public static final String CLUSTERNAME = randomName();

	public static final Vertx vertx = Vertx.vertx();

	public static final int STARTUP_TIMEOUT = 30;

	/**
	 * Generate a random string with the prefix "random"
	 * 
	 * @return
	 */
	public static String randomName() {
		return "random" + System.currentTimeMillis();
	}

	protected OrientDBServer addNode(String clusterName, String nodeName, boolean clearFolders) {
		OrientDBServer server = new OrientDBServer(vertx)
			.withClusterName(clusterName)
			.withNodeName(nodeName)
			.waitForStartup();
		if (clearFolders) {
			server.withClearFolders();
		}
		server.start();
		return server;
	}

}
