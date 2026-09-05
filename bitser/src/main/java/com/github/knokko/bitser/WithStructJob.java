package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;
import com.github.knokko.bitser.field.FunctionContext;

record WithStructJob(Object structObject, BitStructWrapper<?> structInfo, RecursionNode node) {

	void register(AbstractReferenceTracker references, FunctionContext functionContext) {
		for (SingleClassWrapper structClass : structInfo.classHierarchy) {
			for (var field : structClass.getFields(false)) {
				String fieldName = field.classField().getName();
				try {
					Object value = field.classField().get(structObject);
					if (value == null) continue;

					field.bitField().registerReferenceTargets(references, value, node, fieldName);
					String label = field.bitField().field.referenceTargetLabel;
					if (label != null) references.registerTarget(label, value);
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(fieldName), failed);
				}
			}

			for (var function : structClass.functions) {
				String functionName = function.classMethod().getName();
				try {
					Object value = function.computeValue(structObject, functionContext);
					if (value == null) continue;

					function.bitField().registerReferenceTargets(references, value, node, functionName);
					String label = function.bitField().field.referenceTargetLabel;
					if (label != null) references.registerTarget(label, value);
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(functionName), failed);
				}
			}
		}
	}
}
