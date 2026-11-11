package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

import java.lang.reflect.Array;

record WithArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {

	void register(AbstractReferenceTracker references) {
		int length = Array.getLength(array);
		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			if (element == null) continue;

			elementsWrapper.registerReferenceTargets(references, element, node, "elements");
			if (elementsWrapper.field.referenceTargetLabel != null) {
				try {
					references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace("elements"), failed);
				}
			}
		}
	}
}
