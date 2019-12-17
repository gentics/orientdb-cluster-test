package de.jotschi.orientdb.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class AbstractClusterTest {

	static {
		System.setProperty("storage.wal.allowDirectIO", "false");
	}

	public static final String CATEGORY = "Category";

	public static final String PRODUCT = "Product";

	public static final String PRODUCT_INFO = "ProductInfo";

	protected Database db;

	private final Random randr = new Random();

	public final List<Object> productIds = new ArrayList<>();

	public List<Object> categoryIds = new ArrayList<>();

	public String nodeName;

	protected void setup(String name, String httpPort, String binPort) throws Exception {
		this.nodeName = name;
		FileUtils.deleteDirectory(new File("target/" + name));
		initDB(name, "target/" + name, httpPort, binPort);
	}

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

	public Vertex createProduct(OrientBaseGraph tx) {
		Vertex v = tx.addVertex("class:" + PRODUCT);
		v.setProperty("name", "SOME VALUE" + System.currentTimeMillis());
		return v;
	}

	public Vertex createProductInfo(OrientBaseGraph tx) {
		Vertex v = tx.addVertex("class:" + PRODUCT_INFO);
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
		Vertex info = createProductInfo(tx);
		Edge edge = product.addEdge("HAS_INFO", info);
		edge.setProperty("name", "Value" + System.currentTimeMillis());
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

	public void updateRandomProduct() {

	}

	public void triggerLoad(Runnable command) throws Exception {
		// Now continue to update the products concurrently
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		System.out.println("Press any key to start load");
		System.in.read();
		executor.scheduleAtFixedRate(command, 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		executor.scheduleAtFixedRate(command, 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		executor.scheduleAtFixedRate(command, 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		executor.scheduleAtFixedRate(command, 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Stopping threads.");
		executor.shutdown();
		Thread.sleep(1000);
		System.out.println("Timer stopped.");
		System.out.println(
			"Press any key to update product one more time. This time no lock error should occure since the other TX's have been terminated.");

		System.in.read();
		productInserter();
	}

	public void productInserter() {
		try {
			tx(tx -> {
				Vertex product = insertProduct(tx);
				product.setProperty("name", nodeName + "@" + System.currentTimeMillis());
				System.out.println("Insert " + product.getId());
			});
			System.out.println("Inserted");
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
	}

}
