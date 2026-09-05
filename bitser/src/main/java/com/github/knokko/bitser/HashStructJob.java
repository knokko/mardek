package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

record HashStructJob(Object structObject, BitStructWrapper<?> wrapper, RecursionNode node) {

	void hash(HashComputer computer) {
		for (var structClass : wrapper.classHierarchy) {
			for (var field : structClass.fields) {
				String fieldName = field.classField().getName();
				try {
					Object value = field.classField().get(structObject);
					field.bitField().hashCode(computer, value, node, fieldName);
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(fieldName), failed);
				}
			}
		}
	}
}
