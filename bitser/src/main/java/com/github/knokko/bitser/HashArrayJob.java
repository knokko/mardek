package com.github.knokko.bitser;

import java.lang.reflect.Array;

record HashArrayJob(
		Object array, BitFieldWrapper elementsWrapper,
		RecursionNode node, String description
) {

	void hash(HashComputer computer) {
		int length = Array.getLength(array);
		for (int index = 0; index < length; index++) {
			Object element = Array.get(array, index);
			elementsWrapper.hashCode(computer, element, node, description);
		}
	}
}
