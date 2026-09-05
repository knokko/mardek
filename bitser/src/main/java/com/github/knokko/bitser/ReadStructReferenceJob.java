package com.github.knokko.bitser;

import java.lang.reflect.Field;

record ReadStructReferenceJob(
		Object structObject, Field classField,
		ReferenceFieldWrapper fieldWrapper, RecursionNode node
) {

	void resolve(Deserializer deserializer) throws Throwable {
		classField.set(structObject, deserializer.references.get(fieldWrapper, deserializer.input));
	}
}
