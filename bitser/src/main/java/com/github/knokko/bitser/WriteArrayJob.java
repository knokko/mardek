package com.github.knokko.bitser;

import java.lang.reflect.Array;

record WriteArrayJob(Object array, BitFieldWrapper elementsWrapper, RecursionNode node, String nullErrorMessage) {

	void write(Serializer serializer) throws Throwable {
		int length = Array.getLength(array);
		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			if (WriteHelper.writeOptional(
					serializer.output, element, elementsWrapper.field.optional, nullErrorMessage
			)) continue;

			serializer.output.pushContext(node, "elements");
			elementsWrapper.write(serializer, element, node, "elements");
			serializer.output.popContext(node, "elements");
			if (elementsWrapper.field.referenceTargetLabel != null) {
				serializer.references.registerTarget(elementsWrapper.field.referenceTargetLabel, element);
			}
		}
	}
}
