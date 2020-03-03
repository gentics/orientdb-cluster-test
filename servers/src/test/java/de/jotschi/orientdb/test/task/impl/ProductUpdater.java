package de.jotschi.orientdb.test.task.impl;

import com.orientechnologies.common.concur.ONeedRetryException;
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
	public void runTask() {
		try {
			test.tx(tx -> {
				// Case A:
				String randomId = Utils.randomUUID();
				Vertex product = test.getRandomProduct(tx);
				product.setProperty("name", test.nodeName + "@" + System.currentTimeMillis());
				// Set a random property
				product.setProperty(randomId, randomId);

				// Case B:
				Vertex existingInfo = test.getRandomProductInfo(tx);
				existingInfo.addEdge(AbstractClusterTest.HAS_INFO, product);

				// Case C:
				Vertex info = test.createProductInfo(tx, Utils.randomUUID());
				product.addEdge(AbstractClusterTest.HAS_INFO, info);

				// Case D:
				Vertex existingInfo2 = test.getRandomProductInfo(tx);
				System.out.println("Deleting " + existingInfo2.getId());
				existingInfo2.remove();

				System.out.println("Updating " + product.getId());
			});
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
	}

}
