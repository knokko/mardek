package com.github.knokko.bitser;

import java.lang.reflect.Array;

record DeepCopyArrayReferenceJob(
		Object destinationArray, Object originalArray, RecursionNode node
) {

	void resolve(DeepCopyMachine machine) {
		int length = Array.getLength(destinationArray);
		for (int index = 0; index < length; index++) {
			Object originalReference = Array.get(originalArray, index);
			if (originalReference != null) {
				Array.set(destinationArray, index, machine.references.convert(originalReference));
			}
		}
	}
}
