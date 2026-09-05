package com.github.knokko.bitser;

import java.lang.reflect.Array;

record WithArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {

	void register(AbstractReferenceTracker references) {
		int length = Array.getLength(array);
		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			if (element == null) continue;

			elementsWrapper.registerReferenceTargets(references, element, node, "elements");
			if (elementsWrapper.field.referenceTargetLabel != null) {
				references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
