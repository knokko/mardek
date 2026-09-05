package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;
import com.github.knokko.bitser.field.FunctionContext;

import java.util.HashMap;
import java.util.Map;

record DeepCopyStructJob(BitStructWrapper<?> wrapper, Object source, Object destination, RecursionNode node) {

	void copy(DeepCopyMachine machine) {
		for (var classWrapper : wrapper.classHierarchy) {
			for (var field : classWrapper.fields) {
				try {
					Object sourceValue = field.classField().get(source);
					if (sourceValue == null) continue;
					if (field.bitField() instanceof ReferenceFieldWrapper) {
						machine.structReferenceJobs.add(new DeepCopyStructReferenceJob(
								destination, field.classField(), sourceValue, node
						));
					} else {
						Object destinationValue = field.bitField().deepCopy(sourceValue, machine, node, field.classField().getName());
						field.classField().set(destination, destinationValue);
						if (field.bitField().field.referenceTargetLabel != null) {
							machine.references.register(sourceValue, destinationValue);
						}
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(field.classField().getName()), failed);
				}
			}
		}

		if (destination instanceof BitPostInit postInit) {
			var functionContext = new FunctionContext(
					machine.bitser, false, machine.withParameters
			);

			Map<Class<?>, Object[]> functionValues = new HashMap<>();
			for (var classWrapper : wrapper.classHierarchy) {
				int maxID = -1;
				for (var function : classWrapper.functions) {
					maxID = Math.max(maxID, function.id());
				}
				Object[] theseFunctionValues = new Object[maxID + 1];
				functionValues.put(classWrapper.myClass, theseFunctionValues);

				for (var function : classWrapper.functions) {
					try {
						theseFunctionValues[function.id()] = function.computeValue(source, functionContext);
					} catch (Throwable failed) {
						throw new RecursionException(node.generateTrace(function.classMethod().getName()), failed);
					}
				}
			}
			var context = new BitPostInit.Context(
					machine.bitser, false, functionValues,
					null, machine.withParameters
			);
			machine.postInitJobs.add(new PostInitJob(postInit, context, node));
		}
	}
}
