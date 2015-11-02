import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class Database {

	private OrientGraphFactory factory;
	private String nodeName;

	public Database(String nodeName) {
		this.nodeName = nodeName;
		this.factory = new OrientGraphFactory("plocal:" + getPath()).setupPool(5, 100);
	}

	public String getPath() {
		return new File("target/" + nodeName).getAbsolutePath();
	}

	public OrientGraphFactory getFactory() {
		return factory;
	}

	private InputStream getOrientServerConfig() throws IOException {
		InputStream configIns = getClass().getResourceAsStream("/config/orientdb-server-config.xml");
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		configString = configString.replaceAll("%PLUGIN_DIRECTORY%", "orient-plugins");
		configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "finest");
		configString = configString.replaceAll("%FILE_LOG_LEVEL%", "fine");
		configString = configString.replaceAll("%NODENAME%", nodeName);
		configString = configString.replaceAll("%DB_PATH%", "plocal:" + getPath());
		InputStream stream = new ByteArrayInputStream(configString.getBytes(StandardCharsets.UTF_8));
		return stream;
	}

	public void startOrientServer() throws Exception {
		OServer server = OServerMain.create();

		InputStream ins = getClass().getResourceAsStream("/plugins/studio-2.1.zip");
		File pluginDirectory = new File("orient-plugins");
		pluginDirectory.mkdirs();
		IOUtils.copy(ins, new FileOutputStream(new File(pluginDirectory, "studio-2.1.zip")));

		server.startup(getOrientServerConfig());
		OServerPluginManager manager = new OServerPluginManager();
		manager.config(server);
		server.activate();
		manager.startup();
	}

}
