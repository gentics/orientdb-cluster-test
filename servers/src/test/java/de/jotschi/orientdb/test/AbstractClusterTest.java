package de.jotschi.orientdb.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import de.jotschi.orientdb.test.task.LoadTask;

public abstract class AbstractClusterTest {

	static {
		// Disable direct IO (My dev system uses ZFS. Otherwise the test will not run)
		System.setProperty("storage.wal.allowDirectIO", "false");
	}

	// Define the test parameter
	static final long txDelay = 0;
	static final boolean lockTx = true;
	static final boolean lockForDBSync = false;

	public static final String HAS_PRODUCT = "HAS_PRODUCT";

	public static final String HAS_INFO = "HAS_INFO";

	public static final String BASE = "Base";

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
				e.printStackTrace();
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

	public Vertex createProduct(OrientBaseGraph tx, String uuid) {
		Vertex v = tx.addVertex("class:" + PRODUCT);
		v.setProperty("uuid", uuid);
		v.setProperty("name", "SOME VALUE" + System.currentTimeMillis());
		return v;
	}

	public Vertex createProductInfo(OrientBaseGraph tx, String uuid) {
		Vertex v = tx.addVertex("class:" + PRODUCT_INFO);
		v.setProperty("uuid", uuid);
		v.setProperty("name", "SOME VALUE" + System.currentTimeMillis());
		return v;
	}

	public Vertex insertProduct(OrientBaseGraph tx, String productUuid, String infoUuid) {
		Vertex product = createProduct(tx, productUuid);
		Vertex info = createProductInfo(tx, infoUuid);
		Edge edge = product.addEdge(HAS_INFO, info);
		edge.setProperty("name", "Value" + System.currentTimeMillis());
		// Add product to all categories
		// for (Vertex category : tx.getVertices("@class", CATEGORY)) {
		// category.addEdge(HAS_PRODUCT, product);
		// }
		productIds.add(product.getId());
		return product;
	}

	public Vertex getRandomProduct(OrientBaseGraph tx) {
		Iterable<Vertex> it = tx.getVerticesOfClass(AbstractClusterTest.PRODUCT);
		List<Vertex> vertices = new ArrayList<>();
		it.forEach(v -> {
			vertices.add(v);
		});

		return vertices.get(randr.nextInt(vertices.size()));
	}

	public Vertex getRandomProductInfo(OrientBaseGraph tx) {
		Iterable<Vertex> it = tx.getVerticesOfClass(AbstractClusterTest.PRODUCT_INFO);
		List<Vertex> vertices = new ArrayList<>();
		it.forEach(v -> {
			vertices.add(v);
		});
		if (vertices.isEmpty()) {
			return null;
		}

		return vertices.get(randr.nextInt(vertices.size()));
	}

	public void deleteAllProductInfos(OrientBaseGraph tx) {
		Iterable<Vertex> it = tx.getVerticesOfClass(AbstractClusterTest.PRODUCT_INFO);
		it.forEach(v -> {
			v.remove();
		});
	}

	public void triggerSlowLoad(LoadTask task) throws Exception {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		System.out.println("Press any key to start load");
		System.in.read();
		System.out.println("Invoking task execution #1");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 5000, TimeUnit.MILLISECONDS);
	}
	
	public void triggerLoad(LoadTask task) throws Exception {

		// Now continue to invoke task
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
		System.out.println("Press any key to start load");
		System.in.read();
		System.out.println("Invoking task execution #1");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 500, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Invoking task execution #2");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Invoking task execution #3");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 20, TimeUnit.MILLISECONDS);
		System.in.read();
		System.out.println("Invoking task execution #4");
		executor.scheduleAtFixedRate(() -> task.runTask(txDelay, lockTx, lockForDBSync), 100, 20, TimeUnit.MILLISECONDS);

		System.out.println("Press any key to shutdown the execution");
		System.in.read();
		System.out.println("Stopping threads.");
		executor.shutdown();
		Thread.sleep(1000);
		System.out.println("Timer stopped.");
		System.out.println(
			"Press any key to update product one more time. This time no lock error should occure since the other TX's have been terminated.");

		System.in.read();
		task.runTask(txDelay, lockTx, lockForDBSync);
	}

	public Consumer<OrientVertexType> nameTypeModifier() {
		return (vertexType) -> {
			String typeName = vertexType.getName();
			String fieldKey = "name";
			vertexType.createProperty(fieldKey, OType.STRING);
			boolean unique = false;
			String indexName = typeName + "_name";
			vertexType.createIndex(indexName.toLowerCase(),
				unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString(),
				null, new ODocument().fields("ignoreNullValues", true), new String[] { fieldKey });
		};
	}

	public Consumer<OrientVertexType> uuidTypeModifier() {
		return (vertexType) -> {
			String typeName = vertexType.getName();
			String fieldKey = "uuid";
			vertexType.createProperty(fieldKey, OType.STRING);
			String indexName = typeName + "_uuid";
			vertexType.createIndex(indexName.toLowerCase(),
				OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString(),
				null, new ODocument().fields("ignoreNullValues", true), new String[] { fieldKey });
		};
	}

	protected void waitAndShutdown() throws IOException {
		System.out.println("Press any key to shutdown the instance");
		System.in.read();
		Utils.sleep(5000);
		db.getServer().shutdown();

	}

	public void insertProducts(long nProducts) {
		tx(tx -> {
			for (int i = 0; i < nProducts; i++) {
				insertProduct(tx, Utils.randomUUID(), Utils.randomUUID());
			}
			return null;
		});
		System.out.println("Inserted " + nProducts + " products..");
	}

	public void createCategories(long nCategories) {
		tx(tx -> {
			for (int i = 0; i < nCategories; i++) {
				Object id = tx.addVertex("class:" + CATEGORY).getId();
				categoryIds.add(id);
				System.out.println("Create category " + id);
			}
			return null;
		});
		System.out.println("Created " + nCategories + " categories...");
	}

	public Database getDb() {
		return db;
	}

}
