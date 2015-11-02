import org.junit.Test;

public class OrientDBClusterTest extends AbstractClusterTest  {

	private final String nodeName = "nodeA";

	@Test
	public void testCluster() throws Exception {
		start(nodeName);
	}
}
