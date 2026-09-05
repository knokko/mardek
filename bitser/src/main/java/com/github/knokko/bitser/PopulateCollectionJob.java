package com.github.knokko.bitser;

import java.util.Collection;
import java.util.Collections;

class PopulateCollectionJob extends PopulateJob {

	final Collection<?> collection;
	Object[] elements;

	PopulateCollectionJob(Collection<?> collection, Object[] elements, RecursionNode node) {
		super(node);
		this.collection = collection;
		this.elements = elements;
	}

	void populate() {
		//noinspection unchecked
		Collections.addAll((Collection<Object>) collection, elements);
	}
}
