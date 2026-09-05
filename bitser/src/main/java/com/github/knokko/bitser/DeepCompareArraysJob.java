package com.github.knokko.bitser;

import java.lang.reflect.Array;

record DeepCompareArraysJob(
		Object arrayA, Object arrayB, BitFieldWrapper elementsWrapper,
		RecursionNode node, String description
) {

	boolean certainlyNotEqual(DeepComparator comparator) {
		int length = Array.getLength(arrayA);
		if (length != Array.getLength(arrayB)) return true;

		for (int index = 0; index < length; index++) {
			Object elementA = Array.get(arrayA, index);
			Object elementB = Array.get(arrayB, index);

			if (elementA == null && elementB == null) continue;
			if (elementA == null || elementB == null) return true;

			if (elementsWrapper.certainlyNotEqual(comparator, elementA, elementB, node, description)) return true;
			if (elementsWrapper.field.referenceTargetLabel != null) {
				comparator.referenceTargetMapping.put(elementA, elementB);
			}
		}

		return false;
	}
}
