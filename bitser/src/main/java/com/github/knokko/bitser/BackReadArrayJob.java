package com.github.knokko.bitser;

import java.lang.reflect.Array;

record BackReadArrayJob(Object array, BitFieldWrapper elementsWrapper, BitFieldWrapper modernElementsWrapper, RecursionNode node) {

	void read(BackDeserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		var readParameters = new BackReadParameters(deserializer, modernElementsWrapper, node, "elements");
		for (int index = 0; index < size; index++) {
			if (ReadHelper.readOptional(deserializer.input, elementsWrapper.field.optional)) continue;

			deserializer.input.pushContext(node, "elements");
			Object element = elementsWrapper.read(readParameters);
			deserializer.input.popContext(node, "elements");

			Array.set(array, index, element);
			if (elementsWrapper.field.referenceTargetLabel != null) {
				deserializer.references.registerLegacyTarget(
						elementsWrapper.field.referenceTargetLabel, element
				);
			}
		}
	}
}
