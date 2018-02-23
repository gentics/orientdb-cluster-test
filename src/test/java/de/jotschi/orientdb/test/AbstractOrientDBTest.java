package de.jotschi.orientdb.test;

public class AbstractOrientDBTest {

	public static final int STARTUP_TIMEOUT = 30;
	
	/**
	 * Generate a random string with the prefix "random"
	 * 
	 * @return
	 */
	public static String randomName() {
		return "random" + System.currentTimeMillis();
	}
}
