package com.github.knokko.bitser;

record WriteStructReferenceJob(Object value, ReferenceFieldWrapper fieldWrapper, RecursionNode node) {

	void save(Serializer serializer) throws Throwable {
		serializer.references.save(fieldWrapper, value, serializer.output);
	}
}
