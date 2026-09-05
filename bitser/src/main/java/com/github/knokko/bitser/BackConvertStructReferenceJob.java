package com.github.knokko.bitser;

record BackConvertStructReferenceJob(
		Object[] modernValues, int id,
		Object legacyReference, RecursionNode node
) {

	void convert(BackDeserializer deserializer) throws Throwable {
		modernValues[id] = deserializer.references.getModernFromRaw(legacyReference);
	}
}
