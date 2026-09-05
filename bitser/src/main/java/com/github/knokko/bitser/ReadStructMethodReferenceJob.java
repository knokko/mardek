package com.github.knokko.bitser;

record ReadStructMethodReferenceJob(
		Object[] functionValues, int functionID,
		ReferenceFieldWrapper fieldWrapper, RecursionNode node
) {
	void resolve(Deserializer deserializer) throws Throwable {
		functionValues[functionID] = deserializer.references.get(fieldWrapper, deserializer.input);
	}
}
