package de.jotschi.orientdb.test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

public class Database {

	private OrientGraphFactory factory;
	private String url;
	private String username;
	private String password;

	public Database(String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public void addEdgeType(Supplier<OrientGraphNoTx> txProvider, String label, String superTypeName) {
		System.out.println("Adding edge type for label {" + label + "}");

		OrientGraphNoTx noTx = txProvider.get();
		try {
			OrientEdgeType edgeType = noTx.getEdgeType(label);
			if (edgeType == null) {
				String superClazz = "E";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				edgeType = noTx.createEdgeType(label, superClazz);

				// Add index
				String fieldKey = "name";
				edgeType.createProperty(fieldKey, OType.STRING);
				boolean unique = false;
				String indexName = label + "_name";
				edgeType.createIndex(indexName.toLowerCase(),
					unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString(),
					null, new ODocument().fields("ignoreNullValues", true), new String[] { fieldKey });
			}
		} finally {
			noTx.shutdown();
		}

	}

	public void addVertexType(Supplier<OrientGraphNoTx> txProvider, String typeName, String superTypeName,
		Consumer<OrientVertexType> typeModifier) {

		System.out.println("Adding vertex type for class {" + typeName + "}");

		OrientGraphNoTx noTx = txProvider.get();
		try {
			OrientVertexType vertexType = noTx.getVertexType(typeName);
			if (vertexType == null) {
				String superClazz = "V";
				if (superTypeName != null) {
					superClazz = superTypeName;
				}
				vertexType = noTx.createVertexType(typeName, superClazz);

				if (typeModifier != null) {
					typeModifier.accept(vertexType);
				}
			}
		} finally {
			noTx.shutdown();
		}
	}

	public OrientGraph getTx() {
		return factory.getTx();
	}

	public OrientGraphNoTx getNoTx() {
		return factory.getNoTx();
	}

	public void create(String name) {
		try (OrientDB orientDB = new OrientDB("remote:" + url, username, password, OrientDBConfig.defaultConfig())) {
			orientDB.createIfNotExists(name, ODatabaseType.PLOCAL);
		}
	}

	public void openRemotely(String name) {
		factory = new OrientGraphFactory("remote:" + url + "/" + name, username, password).setupPool(16, 100);
	}

	public void close() {
		if (factory != null) {
			factory.close();
			factory = null;
		}
	}

}
