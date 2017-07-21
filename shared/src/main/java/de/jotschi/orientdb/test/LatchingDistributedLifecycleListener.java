package de.jotschi.orientdb.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.orientechnologies.orient.server.distributed.ODistributedLifecycleListener;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;

public class LatchingDistributedLifecycleListener implements ODistributedLifecycleListener {

	private CountDownLatch nodeJoinLatch = new CountDownLatch(1);

	private String selfNodeName;

	public LatchingDistributedLifecycleListener(String selfNodeName) {
		this.selfNodeName = selfNodeName;
	}

	@Override
	public boolean onNodeJoining(String iNode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onNodeJoined(String iNode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onNodeLeft(String iNode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDatabaseChangeStatus(String iNode, String iDatabaseName, DB_STATUS iNewStatus) {
		if ("storage".equals(iDatabaseName) && iNewStatus == DB_STATUS.ONLINE && iNode.equals(selfNodeName)) {
			nodeJoinLatch.countDown();
		}
	}

	public boolean waitForMainGraphDB(int timeout, TimeUnit unit) throws InterruptedException {
		return nodeJoinLatch.await(timeout, unit);
	}

}