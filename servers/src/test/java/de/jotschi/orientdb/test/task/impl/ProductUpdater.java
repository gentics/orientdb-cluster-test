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
 * 
 * E) Delete all product Infos
 */
public class ProductUpdater extends AbstractLoadTask {

	public ProductUpdater(AbstractClusterTest test) {
		super(test);
	}

	@Override
	public void runTask() {
		try {
			test.tx(tx -> {

				Vertex p1 = test.getRandomProduct(tx);
				Object id = p1.getId();
				// Read data
				for (int i = 0; i < 10; i++) {
					Vertex p = test.getRandomProduct(tx);
					p.getProperty("name");
				}
				// Test nested tx
				test.tx(tx2 -> {
					// Case A:
					Vertex product = tx2.getVertex(id);
					String randomId = Utils.randomUUID();
					product.setProperty("name", test.nodeName + "@" + System.currentTimeMillis());
					// Set a random property
					product.setProperty(randomId, randomId);

					// Case B:
					Vertex existingInfo = test.getRandomProductInfo(tx2);
					if (existingInfo != null) {
						existingInfo.addEdge(AbstractClusterTest.HAS_INFO, product);
					}

					// Case C:
					Vertex info = test.createProductInfo(tx2, Utils.randomUUID());
					product.addEdge(AbstractClusterTest.HAS_INFO, info);

					// Case D:
					Vertex existingInfo2 = test.getRandomProductInfo(tx2);
					System.out.println("Deleting " + existingInfo2.getId());
					existingInfo2.remove();
					// System.out.println("Updating " + product.getId());

					// Case E:
					// test.deleteAllProductInfos(tx2);
				});
			});
		} catch (ONeedRetryException e) {
			e.printStackTrace();
			System.out.println("Ignoring ONeedRetryException - normally we would retry the action.");
		}
	}

}
