package de.jotschi.orientdb.test;

import static com.tinkerpop.blueprints.Direction.IN;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class AbstractClusterTest {

	private static final long PRODUCT_COUNT = 1L;

	public static final String CATEGORY = "Category";

	public static final String PRODUCT = "Product";

	protected Database db;

	protected Vertx vertx;

	private final Random randr = new Random();

	public final List<Object> productIds = new ArrayList<>();

	public Object categoryId;

	static {
		OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(Integer.MAX_VALUE);
	}

	public void initDB(String name, String graphDbBasePath, String httpPort, String binPort) throws Exception {
		db = new Database(name, graphDbBasePath, httpPort, binPort);
	}

	public void startVertx() throws InterruptedException {
		VertxOptions options = new VertxOptions();
		options.setClustered(true);
		options.setBlockedThreadCheckInterval(Integer.MAX_VALUE);
		CountDownLatch latch = new CountDownLatch(1);
		Vertx.clusteredVertx(options, rh -> {
			System.out.println("Vert.x Joined Cluster");
			vertx = rh.result();
			latch.countDown();
		});
		latch.await(10, TimeUnit.SECONDS);
	}

	public <T> T tx(Function<OrientBaseGraph, T> handler) {
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
			tx.shutdown();
		}
	}

	public void tx(Consumer<OrientBaseGraph> handler) {
		tx(tx -> {
			handler.accept(tx);
			return null;
		});
	}

	public void updateRandomEdge(OrientBaseGraph tx, OrientVertex category) {
		for (Edge edge : category.getEdges(Direction.OUT, "TEST")) {
			double rnd = Math.random();
			if (rnd > 0.98) {
				Vertex inV = edge.getVertex(IN);
				category.addEdge("TEST2", inV);
				System.out.println("Adding edge");
			}
		}
	}

	public void updateAllProducts(OrientBaseGraph tx) {
		for (Vertex v : tx.getVertices("@class", PRODUCT)) {
			v.setProperty("name", System.currentTimeMillis());
		}
	}

	public Object addProduct(OrientBaseGraph tx, OrientVertex category) {
		Vertex v = tx.addVertex("class:" + PRODUCT);
		v.setProperty("name", "SOME VALUE");
		category.addEdge("TEST", v);
		return v.getId();
	}

	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void loadProductIds() {
		tx(tx -> {
			OrientVertex category = tx.getVertex(categoryId);
			category.getVertices(Direction.OUT, "TEST").forEach(v -> {
				productIds.add(v.getId());
			});
			return null;
		});
	}

	public void createCategory() {
		categoryId = tx(tx -> {
			return tx.addVertex("class:" + CATEGORY).getId();
		});
		System.out.println("Created category " + categoryId);
	}

	public void loadCategoryId() {
		categoryId = tx(tx -> {
			return tx.getVertices("@class", CATEGORY).iterator().next().getId();
		});
	}

	public void insertProducts() {
		tx(tx -> {
			for (int i = 0; i < PRODUCT_COUNT; i++) {
				OrientVertex category = getCategory(tx);
				productIds.add(addProduct(tx, category));
			}
			return null;
		});
		System.out.println("Inserted " + PRODUCT_COUNT + " products");
	}

	public OrientVertex getCategory(OrientBaseGraph tx) {
		return tx.getVertex(categoryId);
	}

	public OrientVertex getRandomProduct(OrientBaseGraph tx) {
		return tx.getVertex(productIds.get(randr.nextInt(productIds.size())));
	}

}
