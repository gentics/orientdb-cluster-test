package de.jotschi.orientdb.test;

public class AbstractClusterTest {

	protected Database db;

	public void start(String name) throws Exception {
		db = new Database(name);

		// 1. Start the orient server
		Runnable t = () -> {
			try {
				db.startOrientServer();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		new Thread(t).start();

		// 2. Let the server startup
		System.out.println("Waiting");
		Thread.sleep(10000);
		System.out.println("Waited");

	}
}
