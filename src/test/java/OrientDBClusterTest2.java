import org.junit.Test;

public class OrientDBClusterTest2 extends AbstractClusterTest {

	private final String nodeName = "nodeB";

	@Test
	public void testCluster() throws Exception {
		start(nodeName);
	}
}
