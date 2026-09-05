package com.github.knokko.bitser;

import java.lang.reflect.Field;

record BackConvertLazyToArrayJob(
		ReferenceLazyBits<?> lazy,
		Object[] array, int index,
		Object modernObject, Field modernField,
		RecursionNode node
) {

	void evaluate(BackDeserializer deserializer) throws Exception {
		String[] labels = lazy.labels;
		lazy.potentialTargets = new LazyReferenceTargets[labels.length];
		for (int index = 0; index < labels.length; index++) {
			lazy.potentialTargets[index] = deserializer.references.getLazyTargets(labels[index]);
		}
		array[index] = lazy.get();
		if (modernField != null) modernField.set(modernObject, lazy.get());
	}
}
