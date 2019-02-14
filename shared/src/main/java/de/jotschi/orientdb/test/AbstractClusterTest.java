package de.jotschi.orientdb.test;

import static com.tinkerpop.blueprints.Direction.IN;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class AbstractClusterTest {

	public static final String CATEGORY = "Category";

	public static final String PRODUCT = "Product";

	protected Database db;

	private final Random randr = new Random();

	public final List<Object> productIds = new ArrayList<>();

	public List<Object> categoryIds = new ArrayList<>();

	public void initDB(String name, String graphDbBasePath, String httpPort, String binPort) throws Exception {
		OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(Integer.MAX_VALUE);
		db = new Database(name, graphDbBasePath, httpPort, binPort);
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

	public Vertex createProduct(OrientBaseGraph tx) {
		Vertex v = tx.addVertex("class:" + PRODUCT);
		v.setProperty("name", "SOME VALUE" + System.currentTimeMillis());
		return v;
	}

	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Vertex insertProduct(OrientBaseGraph tx) {
		Vertex product = createProduct(tx);
		// Add product to all categories
		for (Vertex category : tx.getVertices("@class", CATEGORY)) {
			category.addEdge("HAS_PRODUCT", product);
		}
		productIds.add(product.getId());
		return product;
	}

	public OrientVertex getRandomProduct(OrientBaseGraph tx) {
		return tx.getVertex(productIds.get(randr.nextInt(productIds.size())));
	}

}
