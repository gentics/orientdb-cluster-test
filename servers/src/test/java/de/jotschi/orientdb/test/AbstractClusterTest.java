package de.jotschi.orientdb.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class AbstractClusterTest {

	static {
		// Disable direct IO (My dev system uses ZFS. Otherwise the test will not run)
		System.setProperty("storage.wal.allowDirectIO", "false");
	}

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

	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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

	public OrientVertex getRandomProduct(OrientBaseGraph tx) {
		return tx.getVertex(productIds.get(randr.nextInt(productIds.size())));
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
			String productUuid = randomUUID();
			String infoUuid = randomUUID();
			tx(tx -> {
				Vertex product = insertProduct(tx, productUuid, infoUuid);
				product.setProperty("name", nodeName + "@" + System.currentTimeMillis());
				System.out.println("Insert " + product.getId() + " " + productUuid + "/" + infoUuid);
			});
			System.out.println("Inserted " + productUuid + "/" + infoUuid);
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
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

	public static String randomUUID() {
		final UUID uuid = UUID.randomUUID();
		return (digits(uuid.getMostSignificantBits() >> 32, 8) + digits(uuid.getMostSignificantBits() >> 16, 4)
			+ digits(uuid.getMostSignificantBits(), 4) + digits(uuid.getLeastSignificantBits() >> 48, 4)
			+ digits(uuid.getLeastSignificantBits(), 12));
	}

	/**
	 * Returns val represented by the specified number of hex digits.
	 * 
	 * @param val
	 * @param digits
	 * @return
	 */
	private static String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
	}

}
