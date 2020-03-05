package de.jotschi.orientdb.test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.orient.server.distributed.ODistributedLifecycleListener;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;

public class LatchingDistributedLifecycleListener implements ODistributedLifecycleListener {

	private CountDownLatch nodeJoinLatch = new CountDownLatch(1);

	private String selfNodeName;

	private HazelcastInstance hz;

	private Map<String, DB_STATUS> statusMap;

	public LatchingDistributedLifecycleListener(String selfNodeName, HazelcastInstance hz) {
		this.selfNodeName = selfNodeName;
		this.hz = hz;
		this.statusMap = hz.getMap("STATUS_MAP");
	}

	@Override
	public boolean onNodeJoining(String iNode) {
		// Lock lock = hz.getLock("TX_LOCK");
		// lock.lock();
		return false;
	}

	@Override
	public void onNodeJoined(String iNode) {
		// Lock lock = hz.getLock("TX_LOCK");
		// lock.unlock();

	}

	@Override
	public void onNodeLeft(String iNode) {
		statusMap.remove(iNode);
	}

	@Override
	public void onDatabaseChangeStatus(String iNode, String iDatabaseName, DB_STATUS iNewStatus) {
		statusMap.put(iNode, iNewStatus);

		if ("storage".equals(iDatabaseName) && iNewStatus == DB_STATUS.ONLINE && iNode.equals(selfNodeName)) {
			System.out.println("Database is now online on {" + iNode + "}");
			nodeJoinLatch.countDown();
		}
	}

	public boolean waitForMainGraphDB(int timeout, TimeUnit unit) throws InterruptedException {
		return nodeJoinLatch.await(timeout, unit);
	}

}
