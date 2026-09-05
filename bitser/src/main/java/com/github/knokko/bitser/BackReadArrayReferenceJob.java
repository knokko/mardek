package com.github.knokko.bitser;

import java.lang.reflect.Array;

record BackReadArrayReferenceJob(Object array, ReferenceFieldWrapper elementsWrapper, RecursionNode node) {

	void resolve(BackDeserializer deserializer) throws Throwable {
		int size = Array.getLength(array);
		for (int index = 0; index < size; index++) {
			if (ReadHelper.readOptional(deserializer.input, elementsWrapper.field.optional)) continue;
			Array.set(array, index, deserializer.references.getWithOrLegacy(elementsWrapper, deserializer.input));
		}
	}
}
