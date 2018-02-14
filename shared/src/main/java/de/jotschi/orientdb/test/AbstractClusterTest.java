package de.jotschi.orientdb.test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class AbstractClusterTest {

	protected Database db;

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

	private AtomicBoolean isInShutdown = new AtomicBoolean(false);

	public void handeActions(String value) throws IOException {
		while (true) {
			System.out.println();
			System.out.println("[c] Continue and update the vertex with value {" + value + "}.");
			System.out.println("[r] Read the record.");
			System.out.println("[t] Terminate the server.");
			char c = (char) System.in.read();
			switch (c) {
			case 'r':
				OrientGraph tx = db.getTx();
				try {
					Vertex vertex = tx.getVertices().iterator().next();
					String currentName = vertex.getProperty("name");
					if ("version1".equalsIgnoreCase(currentName)) {
						vertex.setProperty("name", value);
					}
				} finally {
					tx.shutdown();
				}
				break;
			case 't':
				System.out.println("Terminating");
				db.closePool();
				db.getServer().shutdown();
				System.exit(0);
				break;
			case 's':
				return;
			default:
				System.out.println("Invalid input..");
			}
		}
	}

	public void registerShutdown() {
		System.out.println("Registering shutdown hook");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					isInShutdown.set(true);
					Thread.sleep(2000);
					System.out.println("Shutting down ...");
					db.closePool();
					db.getServer().shutdown();
				} catch (InterruptedException e) {
				}
			}
		});
	}

	public void readStatus() throws InterruptedException {
		// Continue to read the node
		OrientGraph tx = db.getTx();
		try {
			Vertex vertex = tx.getVertices().iterator().next();
			if (vertex != null) {
				String name = vertex.getProperty("name");
				System.out.println("Current name: " + name);
			}
		} finally {
			tx.shutdown();
		}
		Thread.sleep(1000);
	}

}
