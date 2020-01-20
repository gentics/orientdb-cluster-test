package de.jotschi.orientdb.test.task;

import de.jotschi.orientdb.test.AbstractClusterTest;

public abstract class AbstractLoadTask implements LoadTask {

	protected AbstractClusterTest test;

	public AbstractLoadTask(AbstractClusterTest test) {
		this.test = test;
	}

}
