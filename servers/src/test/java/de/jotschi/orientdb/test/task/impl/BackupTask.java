package de.jotschi.orientdb.test.task.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.tinkerpop.blueprints.Vertex;

import de.jotschi.orientdb.test.AbstractClusterTest;
import de.jotschi.orientdb.test.Database;
import de.jotschi.orientdb.test.Utils;
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
		Lock lock = null;
		if (lockTx) {
			HazelcastInstance hz = test.getDb().getHazelcast();
			lock = hz.getLock(Database.TX_LOCK_KEY);
			lock.lock();
		}
		try {
			test.getDb().backup();
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}

}
