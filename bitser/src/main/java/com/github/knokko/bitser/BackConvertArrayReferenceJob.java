package com.github.knokko.bitser;

import java.lang.reflect.Array;

record BackConvertArrayReferenceJob(
		Object legacyArray, Object modernArray,
		ReferenceFieldWrapper modernWrapper, RecursionNode node
) {

	void convert(BackDeserializer deserializer) {
		int length = Array.getLength(legacyArray);
		for (int index = 0; index < length; index++) {
			Object legacyElement = Array.get(legacyArray, index);
			if (legacyElement != null) {
				Array.set(modernArray, index, deserializer.references.getModernFromRaw(legacyElement));
			}
		}
	}
}
