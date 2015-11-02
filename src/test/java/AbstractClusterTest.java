import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class AbstractClusterTest {

	public void start(String name) throws Exception {
		Database db = new Database(name);
		db.startOrientServer();

		db.setupFactory();
		OrientGraphNoTx graph = db.getFactory().getNoTx();

		for (int i = 0; i < 1000000; i++) {
			if ("nodeA".equalsIgnoreCase(name)) {
				graph.addVertex(null);
			}
			System.out.println("Count: " + graph.countVertices());
			Thread.sleep(1500);
		}
		System.in.read();
	}
}
