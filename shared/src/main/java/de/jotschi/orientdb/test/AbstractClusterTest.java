package de.jotschi.orientdb.test;

import java.io.IOException;
import java.util.Scanner;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class AbstractClusterTest {

	protected Database db;

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

	public void handeActions(String value) throws IOException {
		printHelp(value);
		while (true) {
			char c = (char) System.in.read();
			System.in.read();
			switch (c) {
			case 'c':
				createVertex();
				break;
			case 'r':
				readVertex();
				break;
			case 'd':
				deleteVertex();
				break;
			case 'u':
				updateVertex();
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
		System.out.println("[c] Create a vertex");
		System.out.println("[r] Read the property value of the test vertex.");
		System.out.println("[u] Update the vertex with value {" + value + "}.");
		System.out.println("[d] Delete the vertex");
		System.out.println("[t] Terminate the server.");
		System.out.println("-----------------------------------------------------");
	}

	// CRUD

	public void createVertex() throws IOException {
		System.out.println("Name:");
		String name = readLine();
		OrientGraph tx = db.getTx();
		try {
			OrientVertex v = tx.addVertex("class:Item");
			v.setProperty("name", name);
			System.out.println("Created vertex {" + v.getId() + "} with name {" + name + "}   ");
		} finally {
			tx.shutdown();
		}
	}

	public void readVertex() {
		OrientGraph tx = db.getTx();
		try {
			for (Vertex v : tx.getVertices()) {
				String name = v.getProperty("name");
				System.out.println("Read vertex {" + v.getId() + "} name: " + name);
			}
		} finally {
			tx.shutdown();
		}
	}

	private void updateVertex() throws IOException {
		System.out.println("Name of vertex to be updated:");
		String name = readLine();
		System.out.println("New name:");
		String newName = readLine();
		OrientGraph tx = db.getTx();
		try {
			for (Vertex v : tx.getVertices("name", name)) {
				v.setProperty("name", newName);
				System.out.println("Updated vertex {" + v.getId() + "}");
			}
		} finally {
			tx.shutdown();
		}
	}

	private void deleteVertex() throws IOException {
		System.out.println("Name:");
		String name = readLine();
		OrientGraph tx = db.getTx();
		try {
			for (Vertex v : tx.getVertices("name", name)) {
				System.out.println("Deleting vertex {" + v.getId() + "}");
				v.remove();
			}
		} finally {
			tx.shutdown();
		}
	}

	private String readLine() throws IOException {
		// BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		// String line = br.readLine();
		// return line;
		Scanner s = new Scanner(System.in);
		return s.nextLine();

	}

}
