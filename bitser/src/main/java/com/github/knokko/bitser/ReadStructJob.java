package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.HashMap;
import java.util.Map;

record ReadStructJob(Object structObject, BitStructWrapper<?> structInfo, RecursionNode node) {

	void read(Deserializer deserializer) {
		Map<Class<?>, Object[]> serializedFunctionValues = new HashMap<>();
		for (SingleClassWrapper structClass : structInfo.classHierarchy) {
			Object[] functionValues;
			if (structClass.functions.isEmpty()) functionValues = new Object[0];
			else functionValues = new Object[structClass.functions.get(structClass.functions.size() - 1).id() + 1];
			var wereReferenceFunctions = new boolean[functionValues.length];

			for (var field : structClass.getFields(false)) {
				String fieldName = field.classField().getName();
				try {
					if (field.readsMethodResult()) continue;
					if (ReadHelper.readOptional(deserializer.input, field.bitField().field.optional)) {
						field.classField().set(structObject, null);
						continue;
					}

					if (field.bitField() instanceof ReferenceFieldWrapper) {
						deserializer.structReferenceJobs.add(new ReadStructReferenceJob(
								structObject, field.classField(), (ReferenceFieldWrapper) field.bitField(),
								new RecursionNode(node, fieldName))
						);
					} else {
						deserializer.input.pushContext(node, fieldName);
						Object value = field.bitField().read(deserializer, node, fieldName);
						deserializer.input.popContext(node, fieldName);

						field.classField().set(structObject, value);
						if (field.bitField().field.referenceTargetLabel != null) {
							deserializer.references.registerTarget(field.bitField().field.referenceTargetLabel, value);
						}
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(fieldName), failed);
				}
			}

			for (var function : structClass.functions) {
				String methodName = function.classMethod().getName();
				try {
					if (ReadHelper.readOptional(deserializer.input, function.bitField().field.optional)) continue;
					if (function.bitField() instanceof ReferenceFieldWrapper) {
						wereReferenceFunctions[function.id()] = true;
						deserializer.structMethodReferenceJobs.add(new ReadStructMethodReferenceJob(
								functionValues, function.id(),
								(ReferenceFieldWrapper) function.bitField(),
								new RecursionNode(node, methodName))
						);
					} else {
						deserializer.input.pushContext(node, methodName);
						Object result = function.bitField().read(deserializer, node, methodName);
						functionValues[function.id()] = result;
						deserializer.input.popContext(node, methodName);

						if (function.bitField().field.referenceTargetLabel != null) {
							deserializer.references.registerTarget(function.bitField().field.referenceTargetLabel, result);
						}
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(methodName), failed);
				}
			}

			for (var field : structClass.getFields(false)) {
				String fieldName = field.classField().getName();
				try {
					if (!field.readsMethodResult()) continue;
					if (wereReferenceFunctions[field.id()]) {
						deserializer.populateFieldJobs.add(new PopulateFieldJob(
								functionValues, field.id(),
								structObject, field.classField(),
								new RecursionNode(node, fieldName)
						));
					} else {
						field.classField().set(structObject, functionValues[field.id()]);
					}
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(fieldName), failed);
				}
			}

			if (structObject instanceof BitPostInit) {
				serializedFunctionValues.put(structClass.myClass, functionValues);
			}
		}

		if (structObject instanceof BitPostInit) {
			BitPostInit.Context context = new BitPostInit.Context(
					deserializer.bitser, false, serializedFunctionValues,
					null, deserializer.withParameters
			);
			deserializer.postInitJobs.add(new PostInitJob((BitPostInit) structObject, context, node));
		}
	}
}
