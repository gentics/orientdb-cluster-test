package de.jotschi.orientdb.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class CRUDVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(CRUDVerticle.class);
	private Database db;

	public CRUDVerticle(Database db) {
		this.db = db;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		HttpServer server = vertx.createHttpServer();
		Router router = Router.router(vertx);
		router.route("/").method(HttpMethod.POST).handler(rc -> {
			rc.request().bodyHandler(bh -> {
				JsonObject json = bh.toJsonObject();
				log.info("Command:\n" + json.encodePrettily());
				handeAction(json);
				rc.response().end();
			});
		});

		server.requestHandler(router::accept);
		server.listen(9000, lh -> {
			if (lh.failed()) {
				startFuture.fail(lh.cause());
			} else {
				startFuture.complete();
			}
		});
	}

	public void handeAction(JsonObject json) {
		String command = json.getString("command");
		switch (command) {
		case "create":
			createVertex(json.getString("name"));
			break;
		case "read":
			readVertex();
			break;
		case "delete":
			deleteVertex(json.getString("name"));
			break;
		case "update":
			updateVertex(json.getString("name"), json.getString("newName"));
			break;
		case "terminate":
			System.out.println("Closing pool");
			db.closePool();
			System.out.println("Shutting down orientdb server");
			db.getServer().shutdown();
			System.exit(0);
			break;
		default:
			System.out.println("Invalid input..{" + command + "}");
		}
	}

	public void createVertex(String name) {
		System.out.println("Name:");
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

	private void updateVertex(String name, String newName) {
		System.out.println("Name of vertex to be updated:");
		System.out.println("New name:");
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

	private void deleteVertex(String name) {
		System.out.println("Name:");
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

}
