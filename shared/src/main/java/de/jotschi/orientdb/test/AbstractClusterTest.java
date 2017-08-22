package de.jotschi.orientdb.test;

public class AbstractClusterTest {

	protected Database db;

	public void initDB(String name, String graphDbBasePath) throws Exception {
		db = new Database(name, graphDbBasePath);
	}

}
