package de.jotschi.orientdb.test.task.impl;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.tinkerpop.blueprints.Vertex;

import de.jotschi.orientdb.test.AbstractClusterTest;
import de.jotschi.orientdb.test.Utils;
import de.jotschi.orientdb.test.task.AbstractLoadTask;

public class ProductInserter extends AbstractLoadTask {

	public ProductInserter(AbstractClusterTest test) {
		super(test);
	}

	@Override
	public void runTask(long txDelay) {
		try {
			String productUuid = Utils.randomUUID();
			String infoUuid = Utils.randomUUID();
			test.tx(tx -> {
				Vertex product = test.insertProduct(tx, productUuid, infoUuid);
				product.setProperty("name", test.nodeName + "@" + System.currentTimeMillis());
				System.out.println("Insert " + product.getId() + " " + productUuid + "/" + infoUuid);
			});
			System.out.println("Inserted " + productUuid + "/" + infoUuid);
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
	}
}
