package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FunctionContext;
import com.github.knokko.bitser.exceptions.RecursionException;

record WriteStructJob(Object structObject, BitStructWrapper<?> structInfo, RecursionNode node) {

	private void writeField(
			Serializer serializer, BitFieldWrapper fieldWrapper,
			Object value, String fieldName
	) throws Throwable {
		if (WriteHelper.writeOptional(
				serializer.output, value, fieldWrapper.field.optional,
				"value must not be null"
		)) return;

		if (fieldWrapper instanceof ReferenceFieldWrapper) {
			serializer.structReferenceJobs.add(new WriteStructReferenceJob(
					value, (ReferenceFieldWrapper) fieldWrapper, new RecursionNode(node, fieldName))
			);
		} else {
			serializer.output.pushContext(node, fieldName);
			fieldWrapper.write(serializer, value, node, fieldName);
			serializer.output.popContext(node, fieldName);
			if (fieldWrapper.field.referenceTargetLabel != null) {
				serializer.references.registerTarget(fieldWrapper.field.referenceTargetLabel, value);
			}
		}
	}

	void write(Serializer serializer) {
		FunctionContext functionContext = new FunctionContext(
				serializer.bitser, false, serializer.withParameters
		);

		for (SingleClassWrapper structClass : structInfo.classHierarchy) {
			for (SingleClassWrapper.FieldWrapper field : structClass.getFields(serializer.backwardCompatible)) {
				try {
					if (field.readsMethodResult()) continue;
					Object value = field.classField().get(structObject);
					writeField(serializer, field.bitField(), value, field.classField().getName());
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(field.classField().getName()), failed);
				}
			}

			for (SingleClassWrapper.FunctionWrapper function : structClass.functions) {
				try {
					Object value = function.computeValue(structObject, functionContext);
					writeField(serializer, function.bitField(), value, function.classMethod().getName());
				} catch (Throwable failed) {
					throw new RecursionException(node.generateTrace(function.classMethod().getName()), failed);
				}
			}
		}
	}
}
