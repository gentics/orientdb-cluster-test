package de.jotschi.orientdb.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;

public class Database {

	private String nodeName;
	private String basePath;

	public Database(String nodeName, String basePath) {
		this.nodeName = nodeName;
		this.basePath = basePath;
	}

	private InputStream getOrientServerConfig() throws IOException {
		InputStream configIns = getClass().getResourceAsStream("/config/orientdb-server-config.xml");
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		configString = configString.replaceAll("%PLUGIN_DIRECTORY%", "orient-plugins");
		configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "finest");
		configString = configString.replaceAll("%FILE_LOG_LEVEL%", "fine");
		configString = configString.replaceAll("%DB_PATH%", "plocal:" + escapePath(basePath + "/storage"));
		configString = configString.replaceAll("%NODENAME%", nodeName);
		configString = configString.replaceAll("%DB_PARENT_PATH%", escapePath(basePath));
		InputStream stream = new ByteArrayInputStream(configString.getBytes(StandardCharsets.UTF_8));
		return stream;
	}

	private String escapePath(String path) {
		return StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(new File(path).getAbsolutePath()));
	}

	public void startOrientServer() throws Exception {
		String orientdbHome = new File("").getAbsolutePath();
		System.setProperty("ORIENTDB_HOME", orientdbHome);
		OServer server = OServerMain.create();

		server.startup(getOrientServerConfig());
		OServerPluginManager manager = new OServerPluginManager();
		manager.config(server);
		server.activate();
		manager.startup();
	}

}
