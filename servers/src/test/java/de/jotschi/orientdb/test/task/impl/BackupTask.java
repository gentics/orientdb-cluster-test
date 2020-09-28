package de.jotschi.orientdb.test.task.impl;

import de.jotschi.orientdb.test.AbstractClusterTest;
import de.jotschi.orientdb.test.task.AbstractLoadTask;

/**
 * Update cases:
 * 
 * A) Update existing vertex property
 * 
 * B) Add new edge between existing vertices
 * 
 * C) Add new edge to new vertex
 * 
 * D) Delete a random vertex
 */
public class BackupTask extends AbstractLoadTask {

	public BackupTask(AbstractClusterTest test) {
		super(test);
	}

	@Override
	public void runTask(long txDelay, boolean lockTx, boolean lockForDBSync) {
		test.getDb().backup();
	}

}
