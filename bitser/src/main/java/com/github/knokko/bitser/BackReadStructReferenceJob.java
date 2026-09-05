package com.github.knokko.bitser;

record BackReadStructReferenceJob(
		Object[] legacyValuesArray, int legacyValuesIndex,
		ReferenceFieldWrapper legacyWrapper, RecursionNode node
) {

	void resolve(BackDeserializer deserializer) throws Throwable {
		legacyValuesArray[legacyValuesIndex] = deserializer.references.getWithOrLegacy(
				legacyWrapper, deserializer.input
		);
	}
}
