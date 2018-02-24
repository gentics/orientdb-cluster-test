package de.jotschi.orientdb.test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class CRUDVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(CRUDVerticle.class);
	private Database db;
	private Server server;

	public CRUDVerticle(Server server) {
		this.server = server;
		this.db = server.db;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		HttpServer httpServer = vertx.createHttpServer();
		Router router = Router.router(vertx);
		router.route("/").method(HttpMethod.POST).handler(rc -> {
			rc.request().bodyHandler(bh -> {
				JsonObject json = bh.toJsonObject();
				log.info("Command:\n" + json.encodePrettily());
				JsonObject responseJson = handeAction(rc.response(), json);
				responseJson.put("nodeName", server.getNodeName());
				responseJson.put("clusterName", server.getClusterName());
				rc.response().end(responseJson.encodePrettily());
			});
		});

		httpServer.requestHandler(router::accept);
		httpServer.listen(9000, lh -> {
			if (lh.failed()) {
				startFuture.fail(lh.cause());
			} else {
				startFuture.complete();
			}
		});
	}

	public JsonObject handeAction(HttpServerResponse response, JsonObject json) {
		String command = json.getString("command");
		switch (command) {
		case "create":
			return createVertex(json.getString("name"));
		case "read":
			return readVertex();
		case "delete":
			return deleteVertex(json.getString("name"));
		case "update":
			return updateVertex(json.getString("name"), json.getString("newName"));
		case "terminate":
			log.info("Closing pool");
			db.closePool();
			log.info("Shutting down orientdb server");
			db.getServer().shutdown();
			response.end();
			System.exit(0);
		default:
			log.info("Invalid command: {" + command + "}");
			response.setStatusCode(400);
			return new JsonObject().put("result", "Invalid command");
		}
	}

	public JsonObject createVertex(String name) {
		log.info("Creating vertex  with name: " + name);
		OrientGraph tx = db.getTx();
		try {
			JsonObject json = new JsonObject();
			OrientVertex v = tx.addVertex("class:Item");
			v.setProperty("name", name);
			log.info("Created vertex {" + v.getId() + "} with name {" + name + "}   ");
			json.put("id", v.getId().toString());
			json.put("name", name);
			return json;
		} finally {
			tx.shutdown();
		}
	}

	public JsonObject readVertex() {
		OrientGraph tx = db.getTx();
		try {
			JsonArray array = new JsonArray();
			JsonObject json = new JsonObject().put("vertices", array);
			for (Vertex v : tx.getVertices()) {
				String name = v.getProperty("name");
				log.info("Read vertex {" + v.getId() + "} name: " + name);
				array.add(new JsonObject().put("name", name).put("id", v.getId().toString()));
			}
			return json;
		} finally {
			tx.shutdown();
		}
	}

	private JsonObject updateVertex(String name, String newName) {
		log.info("Name of vertex to be updated:");
		log.info("New name:");
		OrientGraph tx = db.getTx();
		try {
			JsonArray array = new JsonArray();
			JsonObject json = new JsonObject().put("updated", array);
			for (Vertex v : tx.getVertices("name", name)) {
				v.setProperty("name", newName);
				log.info("Updated vertex {" + v.getId() + "}");
				array.add(v.getId().toString());
			}
			return json;
		} finally {
			tx.shutdown();
		}
	}

	private JsonObject deleteVertex(String name) {
		log.info("Name:");
		OrientGraph tx = db.getTx();
		try {
			JsonArray array = new JsonArray();
			JsonObject json = new JsonObject().put("deleted", array);
			for (Vertex v : tx.getVertices("name", name)) {
				log.info("Deleting vertex {" + v.getId() + "}");
				v.remove();
				array.add(v.getId().toString());
			}
			return json;
		} finally {
			tx.shutdown();
		}
	}

}
