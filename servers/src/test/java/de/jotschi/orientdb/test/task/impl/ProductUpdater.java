package de.jotschi.orientdb.test.task.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.tinkerpop.blueprints.Vertex;

import de.jotschi.orientdb.test.AbstractClusterTest;
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
public class ProductUpdater extends AbstractLoadTask {

	public ProductUpdater(AbstractClusterTest test) {
		super(test);
	}

	@Override
	public void runTask(long txDelay, boolean lockTx, boolean lockForDBSync) {
		Lock lock = null;
		if (lockTx) {
			HazelcastInstance hz = test.getDb().getHazelcast();
			lock = hz.getLock("TX_LOCK");
			lock.lock();
		}
		if (lockForDBSync) {
			checkForDB();
		}
		try {
			test.tx(tx -> {

				Vertex p1 = test.getRandomProduct(tx);
				Object id = p1.getId();
				// Read data
				for (int i = 0; i < 10; i++) {
					Vertex p = test.getRandomProduct(tx);
					p.getProperty("name");
				}
				// Case A:
				Vertex product = tx.getVertex(id);
				String randomId = Utils.randomUUID();
				product.setProperty("name", test.nodeName + "@" + System.currentTimeMillis());
				// Set a random property
				product.setProperty(randomId, randomId);

				// Case B:
				Vertex existingInfo = test.getRandomProductInfo(tx);
				if (existingInfo != null) {
					existingInfo.addEdge(AbstractClusterTest.HAS_INFO, product);
				}

				// Case C:
				Vertex info = test.createProductInfo(tx, Utils.randomUUID());
				product.addEdge(AbstractClusterTest.HAS_INFO, info);

				// Case D:
				Vertex existingInfo2 = test.getRandomProductInfo(tx);
				System.out.println("Deleting " + existingInfo2.getId());
				existingInfo2.remove();

				// Simulate long running tx
				try {
					Thread.sleep(txDelay);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			});
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}

	}

	private void checkForDB() {
		HazelcastInstance hz = test.getDb().getHazelcast();
		Map<String, DB_STATUS> map = hz.getMap("STATUS_MAP");
		for (int i = 0; i < 40; i++) {
			boolean foundState = false;
			for (Entry<String, DB_STATUS> entry : map.entrySet()) {
				DB_STATUS status = entry.getValue();
				System.out.println(entry.getKey() + " = " + entry.getValue().name());
				switch (status) {
				case BACKUP:
				case SYNCHRONIZING:
					System.out.println("Locking since " + entry.getKey() + " is in status " + entry.getValue());
					foundState = true;
				default:
					continue;
				}
			}
			if (!foundState) {
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
