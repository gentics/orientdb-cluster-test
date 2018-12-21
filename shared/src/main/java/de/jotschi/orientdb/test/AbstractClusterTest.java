package de.jotschi.orientdb.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.hazelcast.core.HazelcastInstance;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class AbstractClusterTest {

	public static final String CATEGORY = "Category";

	public static final String PRODUCT = "Product";

	protected Database db;

	protected Vertx vertx;

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

	public void startVertx() throws InterruptedException {
		VertxOptions vertxOptions = new VertxOptions();
		vertxOptions.setClustered(true);
		CountDownLatch latch = new CountDownLatch(1);
		HazelcastInstance hazelcast = (HazelcastInstance) db.getHazelcast();
		HazelcastClusterManager manager = new HazelcastClusterManager(hazelcast);
		vertxOptions.setClusterManager(manager);

		Vertx.clusteredVertx(vertxOptions, rh -> {
			System.out.println("Vert.x Joined Cluster");
			vertx = rh.result();
			latch.countDown();
		});
		latch.await(10, TimeUnit.SECONDS);
	}

	public <T> T tx(Function<OrientGraph, T> handler) {
		OrientGraph tx = db.getTx();
		try {
			try {
				T result = handler.apply(tx);
				tx.commit();
				return result;
			} catch (Exception e) {
				// Explicitly invoke rollback as suggested by luigidellaquila
				tx.rollback();
				throw e;
			}
		} finally {
			tx.close();
		}
	}

	public void tx(Consumer<OrientGraph> handler) {
		tx(tx -> {
			handler.accept(tx);
			return null;
		});
	}

	public void updateRandomEdge(OrientGraph tx, Vertex category) {
		category.edges(Direction.OUT, "TEST").forEachRemaining(e -> {
			double rnd = Math.random();
			if (rnd > 0.98) {
				Vertex inV = e.inVertex();
				category.addEdge("TEST2", inV);
				System.out.println("Adding edge");
			}
		});
	}

	public void updateAllProducts(OrientGraph tx) {
		for (Vertex v : tx.traversal().V().has("@class", PRODUCT).toSet()) {
			v.property("name", System.currentTimeMillis());
		}
	}

	public void addProduct(OrientGraph tx, Vertex category) {
		Vertex v = tx.addVertex("class:" + PRODUCT);
		v.property("name", "SOME VALUE");
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
