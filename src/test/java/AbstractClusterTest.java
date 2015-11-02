import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class AbstractClusterTest {

	public void start(String name) throws Exception {
		Database db = new Database(name);
		db.startOrientServer();
		OrientGraphNoTx graph = db.getFactory().getNoTx();

		if ("nodeA".equalsIgnoreCase(name)) {
			for (int i = 0; i < 1000000; i++) {
				graph.addVertex(null);
				Thread.sleep(1500);
			}
		}
		for (int i = 0; i < 100000; i++) {
			System.out.println("Count: " + graph.countVertices());
			Thread.sleep(200);
		}
		System.in.read();
	}
}
