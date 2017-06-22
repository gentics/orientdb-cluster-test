import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

public class ContainersTest {

	@ClassRule
	public static GenericContainer mesh = new GenericContainer("gentics/mesh-demo:latest").withExposedPorts(8080);

	@Test
	public void testInteraction() {
		String meshUrl = mesh.getContainerIpAddress() + ":" + mesh.getMappedPort(8080);
		System.out.println(meshUrl);
	}
}
