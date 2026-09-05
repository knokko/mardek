package com.github.knokko.bitser;

import java.lang.reflect.Field;

record ReadStructReferenceJob(
		SingleClassMutator.Session mutateSession, Field classField,
		ReferenceFieldWrapper fieldWrapper, RecursionNode node
) {

	void resolve(Deserializer deserializer) throws Throwable {
		mutateSession.set(classField, deserializer.references.get(fieldWrapper, deserializer.input));
		mutateSession.finish();
	}
}
