package de.jotschi.orientdb.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class AbstractClusterTest {

	public static final String CATEGORY = "Category";

	public static final String PRODUCT = "Product";

	protected Database db;

	protected Vertx vertx;

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

	public void startVertx() throws InterruptedException {
		VertxOptions options = new VertxOptions();
		options.setClustered(true);
		CountDownLatch latch = new CountDownLatch(1);
		Vertx.clusteredVertx(options, rh -> {
			System.out.println("Vertx Joined Cluster");
			vertx = rh.result();
			latch.countDown();
		});
		latch.await(10, TimeUnit.SECONDS);
	}

	public <T> T tx(Function<OrientBaseGraph, T> handler) {
		OrientGraph tx = db.getTx();
		try {
			T result = handler.apply(tx);
			try {
				tx.commit();
			} catch (Exception e) {
				// Explicitly invoke rollback as suggested by luigidellaquila
				tx.rollback();
				throw e;
			}
			return result;
		} finally {
			tx.shutdown();
		}
	}

	public void tx(Consumer<OrientBaseGraph> handler) {
		tx(tx -> {
			handler.accept(tx);
			return null;
		});
	}

	public void updateAllProducts(OrientBaseGraph tx) {
		for (Vertex v : tx.getVertices("@class", PRODUCT)) {
			v.setProperty("name", System.currentTimeMillis());
		}
	}

	public void addProduct(OrientBaseGraph tx, Object categoryId) {
		OrientVertex category = tx.getVertex(categoryId);
		Vertex v = tx.addVertex("class:" + PRODUCT);
		v.setProperty("name", "SOME VALUE");
		category.addEdge("TEST", v);
	}

	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
