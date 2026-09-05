package com.github.knokko.bitser;

import java.lang.reflect.Array;

record ReadArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node) {

	void read(Deserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (ReadHelper.readOptional(deserializer.input, elementsWrapper.field.optional)) continue;

			deserializer.input.pushContext(node, "elements");
			Object element = elementsWrapper.read(deserializer, node, "elements");
			deserializer.input.popContext(node, "elements");

			Array.set(array, index, element);
			if (elementsWrapper.field.referenceTargetLabel != null) {
				deserializer.references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
