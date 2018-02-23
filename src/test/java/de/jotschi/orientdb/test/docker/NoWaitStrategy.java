package de.jotschi.orientdb.test.docker;

import org.testcontainers.containers.GenericContainer.AbstractWaitStrategy;

public class NoWaitStrategy extends AbstractWaitStrategy {

	@Override
	protected void waitUntilReady() {

	}

}
