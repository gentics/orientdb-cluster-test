package de.jotschi.orientdb.test.docker;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import de.jotschi.orientdb.test.Server;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

/**
 * Test container for a orientdb instance which uses local class files. The image for the container will automatically be rebuild during each startup.
 */
public class OrientDBServer extends GenericContainer<OrientDBServer> {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final Logger log = LoggerFactory.getLogger(OrientDBServer.class);

	private Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

	private HttpClient client;

	private Vertx vertx;

	private static ImageFromDockerfile image = prepareDockerImage(true);

	/**
	 * Action which will be invoked once the orientdb instance is ready.
	 */
	private Runnable startupAction = () -> {
		client = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(getMappedPort(9000)).setDefaultHost("localhost"));
	};

	private StartupLatchingConsumer startupConsumer = new StartupLatchingConsumer(startupAction);

	/**
	 * Name of the node.
	 */
	private String nodeName;

	private boolean initCluster = false;

	private boolean waitForStartup;

	private boolean clearDataFolders = false;

	private Integer debugPort;

	private String clusterName;

	private String extraOpts;

	/**
	 * Create a new docker server
	 * 
	 * @param vertx
	 *            Vert.x instances used to create the rest client
	 */
	public OrientDBServer(Vertx vertx) {
		super(image);
		this.vertx = vertx;
		setWaitStrategy(new NoWaitStrategy());
	}

	@Override
	protected void configure() {
		String dataPath = "/opt/jenkins-slave/" + clusterName + "-" + nodeName + "-data";
		// Ensure that the folder is created upfront. This is important to keep the uid and gids correct.
		// Otherwise the folder would be created by docker using root.

		if (clearDataFolders) {
			try {
				prepareFolder(dataPath);
			} catch (Exception e) {
				fail("Could not setup bind folder {" + dataPath + "}");
			}
		}
		new File(dataPath).mkdirs();
		addFileSystemBind(dataPath, "/data", BindMode.READ_WRITE);

		changeUserInContainer();
		if (initCluster) {
			addEnv(Server.INIT_CLUSTER, "true");
		}
		List<Integer> exposedPorts = new ArrayList<>();
		addEnv(Server.NODE_NAME_ENV, nodeName);
		addEnv(Server.CLUSTER_NAME_ENV, clusterName);

		String javaOpts = null;
		if (debugPort != null) {
			javaOpts = "-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n ";
			exposedPorts.add(8000);
			setPortBindings(Arrays.asList("8000:8000"));
		}
		if (extraOpts != null) {
			if (javaOpts == null) {
				javaOpts = "";
			}
			javaOpts += extraOpts + " ";
		}
		if (javaOpts != null) {
			addEnv("JAVAOPTS", javaOpts);
		}

		exposedPorts.add(9000);

		setExposedPorts(exposedPorts);
		setLogConsumers(Arrays.asList(logConsumer, startupConsumer));
		setStartupAttempts(1);
	}

	private void changeUserInContainer() {
		int uid = 1000;
		try {
			uid = UnixUtils.getUid();
		} catch (IOException e) {
			e.printStackTrace();
		}
		final int id = uid;
		withCreateContainerCmdModifier(it -> it.withUser(id + ":" + id));
	}

	@Override
	public void start() {
		super.start();
		if (waitForStartup) {
			try {
				awaitStartup(300);
			} catch (InterruptedException e) {
				throw new ContainerLaunchException("Container did not not startup on-time", e);
			}
		}
	}

	@Override
	public void stop() {
		log.info("Stopping node {" + getNodeName() + "} of cluster {" + getClusterName() + "} Id: {" + getContainerId() + "}");
		dockerClient.stopContainerCmd(getContainerId()).exec();
		super.stop();
	}

	@Override
	public void close() {
		stop();
	}

	/**
	 * Ensures that an empty folder exists for the given path.
	 * 
	 * @param path
	 * @throws IOException
	 */
	private static void prepareFolder(String path) throws IOException {
		File folder = new File(path);
		FileUtils.deleteDirectory(folder);
		folder.mkdirs();
	}

	/**
	 * Prepare the docker image for the container which will contain all locally found classes.
	 * 
	 * @param enableClustering
	 * @return
	 */
	public static ImageFromDockerfile prepareDockerImage(boolean enableClustering) {
		ImageFromDockerfile dockerImage = new ImageFromDockerfile("orientdb-local", true);
		try {
			String classPathArg = "bin/classes";
			dockerImage.withFileFromPath("bin/classes", new File("target/classes").toPath());

			// Add maven libs
			File libFolder = new File("target/mavendependencies-sharedlibs");
			assertTrue("The library folder {" + libFolder + "} could not be found", libFolder.exists());
			for (File lib : libFolder.listFiles()) {
				String dockerPath = lib.getPath();
				classPathArg += ":bin/" + dockerPath;
			}
			dockerImage.withFileFromPath("bin/target/mavendependencies-sharedlibs", libFolder.toPath());

			// Add sudoers
			dockerImage.withFileFromString("sudoers", "root ALL=(ALL) ALL\n%orientdb ALL=(ALL) NOPASSWD: ALL\n");

			String dockerFile = IOUtils.toString(OrientDBServer.class.getResourceAsStream("/Dockerfile.local"));
			int uid = UnixUtils.getUid();
			// We need to keep the uid of the docker container env and the local test execution env in sync to be able to access the data of the mounted volume.
			dockerFile = dockerFile.replace("%UID%", String.valueOf(uid));
			dockerFile = dockerFile.replace("%CMD%", generateCommand(classPathArg));
			dockerImage.withFileFromString("Dockerfile", dockerFile);

			return dockerImage;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String generateCommand(String classpath) {
		StringBuilder builder = new StringBuilder();
		builder.append("exec");
		builder.append(" ");
		builder.append("java");
		builder.append(" ");
		builder.append("$JAVAOPTS");
		builder.append(" ");
		builder.append("-cp");
		builder.append(" ");
		builder.append(classpath);
		builder.append(" ");
		builder.append("de.jotschi.orientdb.test.Server");
		return builder.toString();
	}

	/**
	 * Block until the startup message has been seen in the container log output.
	 * 
	 * @param timeoutInSeconds
	 * @throws InterruptedException
	 */
	public void awaitStartup(int timeoutInSeconds) throws InterruptedException {
		startupConsumer.await(timeoutInSeconds, SECONDS);
	}

	public HttpClient client() {
		return client;
	}

	/**
	 * Expose the debug port to connect to.
	 * 
	 * @param debugPort
	 *            JNLP debug port. No debugging is enabled when set to null.
	 * @return Fluent API
	 */
	public OrientDBServer withDebug(int debugPort) {
		this.debugPort = debugPort;
		return this;
	}

	/**
	 * Wait until the orientdb instance is ready.
	 * 
	 * @return
	 */
	public OrientDBServer waitForStartup() {
		waitForStartup = true;
		return this;
	}

	/**
	 * Use the provided JVM arguments.
	 * 
	 * @param opts
	 *            Additional JVM options }
	 * @return
	 */
	public OrientDBServer withExtraOpts(String opts) {
		extraOpts = opts;
		return this;
	}

	/**
	 * Set the name of the node.
	 * 
	 * @param name
	 * @return
	 */
	public OrientDBServer withNodeName(String name) {
		this.nodeName = name;
		return this;
	}

	/**
	 * Set the name of the cluster.
	 * 
	 * @param name
	 * @return
	 */
	public OrientDBServer withClusterName(String name) {
		this.clusterName = name;
		return this;
	}

	/**
	 * Set the init cluster flag.
	 * 
	 * @return
	 */
	public OrientDBServer withInitCluster() {
		this.initCluster = true;
		return this;
	}

	/**
	 * Clear the data folder during startup.
	 * 
	 * @return
	 */
	public OrientDBServer withClearFolders() {
		this.clearDataFolders = true;
		return this;
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getClusterName() {
		return clusterName;
	}

	public JsonObject command(JsonObject cmd) throws Exception {
		CompletableFuture<JsonObject> response = new CompletableFuture<JsonObject>();
		client.post("/", rh -> {
			rh.bodyHandler(bh -> {
				log.info(getNodeName() + "=" + bh.toJsonObject().encodePrettily());
				response.complete(bh.toJsonObject());
			});
		}).end(cmd.encodePrettily());
		return response.get(10, TimeUnit.SECONDS);
	}

}
