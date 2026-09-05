package com.github.knokko.bitser;

import java.lang.reflect.Field;

record DeepCopyStructReferenceJob(
		Object structObject, Field classField,
		Object originalReference, RecursionNode node
) {

	void resolve(DeepCopyMachine machine) throws Throwable {
		classField.set(structObject, machine.references.convert(originalReference));
	}
}
