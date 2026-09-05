package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;
import com.github.knokko.bitser.field.FunctionContext;

record CollectFromStructJob(Object structObject, BitStructWrapper<?> structWrapper, RecursionNode node) {

	void collect(InstanceCollector collector) {
		for (var classWrapper : structWrapper.classHierarchy) {
			for (var field : classWrapper.fields) {
				try {
					Object value = field.classField().get(structObject);
					if (value == null) continue;
					if (field.bitField() instanceof ReferenceFieldWrapper) {
						collector.registerReference(value);
					} else {
						collector.register(value);
						field.bitField().collectInstances(collector, value, node, field.classField().getName());
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(field.classField().getName()), failed);
				}
			}

			var functionContext = new FunctionContext(
					collector.bitser, false, collector.withObjects
			);
			for (var function : classWrapper.functions) {
				try {
					Object value = function.computeValue(structObject, functionContext);
					if (value == null) continue;
					if (function.bitField() instanceof ReferenceFieldWrapper) {
						collector.registerReference(value);
					} else {
						collector.register(value);
						function.bitField().collectInstances(collector, value, node, function.classMethod().getName());
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(function.classMethod().getName()), failed);
				}
			}
		}
	}
}
