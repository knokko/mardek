package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

record DeepCompareStructsJob(
		Object structObjectA, Object structObjectB,
		BitStructWrapper<?> structInfo, RecursionNode node
) {

	boolean certainlyNotEqual(DeepComparator comparator) {
		for (var structClass : structInfo.classHierarchy) {
			for (var field : structClass.fields) {
				var fieldName = field.classField().getName();
				try {
					Object valueA = field.classField().get(structObjectA);
					Object valueB = field.classField().get(structObjectB);
					if (valueA == null && valueB == null) continue;
					if (valueA == null || valueB == null) return true;
					if (field.bitField().certainlyNotEqual(comparator, valueA, valueB, node, fieldName)) return true;
					if (field.bitField().field.referenceTargetLabel != null) {
						comparator.referenceTargetMapping.put(valueA, valueB);
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(fieldName), failed);
				}
			}
		}

		return false;
	}
}
