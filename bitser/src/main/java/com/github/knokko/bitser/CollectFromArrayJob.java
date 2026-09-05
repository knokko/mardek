package com.github.knokko.bitser;

import java.lang.reflect.Array;

record CollectFromArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node, String description) {

	void collect(InstanceCollector collector) {
		int length = Array.getLength(array);

		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			if (element == null) continue;
			if (elementsWrapper instanceof ReferenceFieldWrapper) {
				collector.registerReference(element);
			} else {
				collector.register(element);
				elementsWrapper.collectInstances(collector, element, node, description);
			}
		}
	}
}
