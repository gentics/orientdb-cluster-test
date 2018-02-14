package de.jotschi.orientdb.test;

import java.io.IOException;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class AbstractClusterTest {

	protected Database db;

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

	public void handeActions(String value) throws IOException {
		printHelp(value);
		while (true) {

			char c = (char) System.in.read();
			switch (c) {
			case 'r':
				readVertex();
				break;
			case 'u':
				updateVertex(value);
				break;
			case 't':
				System.out.println("Closing pool");
				db.closePool();
				System.out.println("Shutting down orientdb server");
				db.getServer().shutdown();
				System.exit(0);
				break;
			case '\n':
				continue;
			default:
				System.out.println("Invalid input..{" + c + "}");
			}
			printHelp(value);
		}
	}

	private void printHelp(String value) {
		System.out.println();
		System.out.println("-----------------------------------------------------");
		System.out.println("[u] Update the vertex with value {" + value + "}.");
		System.out.println("[r] Read the property value of the test vertex.");
		System.out.println("[t] Terminate the server.");
		System.out.println("-----------------------------------------------------");
	}

	private void updateVertex(String value) {
		OrientGraph tx = db.getTx();
		try {
			Vertex vertex = tx.getVertices().iterator().next();
			vertex.setProperty("name", value);
		} finally {
			tx.shutdown();
		}
	}

	public void readVertex() {
		OrientGraph tx = db.getTx();
		try {
			for (Vertex vertex : tx.getVertices()) {
				String name = vertex.getProperty("name");
				System.out.println("Vertex " + vertex.getId() + " name: " + name);
			}
		} finally {
			tx.shutdown();
		}
	}

}
