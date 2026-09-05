package com.github.knokko.bitser;

import java.lang.reflect.Array;

record DeepCopyArrayJob(
		Object source, Object destination, BitFieldWrapper elementsWrapper,
		RecursionNode node, String description
) {
	void copy(DeepCopyMachine machine) {
		int length = Array.getLength(source);
		for (int index = 0; index < length; index++) {
			Object originalElement = Array.get(source, index);
			if (originalElement == null) continue;
			Object copiedElement = elementsWrapper.deepCopy(originalElement, machine, node, description);
			Array.set(destination, index, copiedElement);
			if (elementsWrapper.field.referenceTargetLabel != null) {
				machine.references.register(originalElement, copiedElement);
			}
		}
	}
}
