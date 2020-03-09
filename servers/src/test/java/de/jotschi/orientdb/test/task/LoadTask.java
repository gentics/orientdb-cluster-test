package de.jotschi.orientdb.test.task;

public interface LoadTask {

	void runTask(long txDelay, boolean lockTx, boolean lockForDBSync);

}
