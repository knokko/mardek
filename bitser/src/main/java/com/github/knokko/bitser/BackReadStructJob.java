package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyClassValues;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.legacy.LegacyUUIDValue;
import com.github.knokko.bitser.exceptions.RecursionException;

import java.util.ArrayList;

record BackReadStructJob(
		LegacyStructInstance legacyObject, LegacyStruct legacyInfo,
		Class<?> modernStruct, RecursionNode node
) {

	private void readFieldsOrFunctions(
			BackDeserializer deserializer, ArrayList<LegacyField> fieldsOrFunctions, boolean isField,
			boolean[] hasValues, Object[] values, SingleClassWrapper modernClass
	) {
		for (LegacyField fieldOrFunction : fieldsOrFunctions) {
			if (fieldOrFunction.readsMethodResult) continue;

			String fieldOrFunctionName = null;
			BitFieldWrapper modernFieldWrapper = null;
			if (modernClass != null) {
				if (isField) {
					for (var modernField : modernClass.fieldsSortedById) {
						if (modernField.id() == fieldOrFunction.id) {
							fieldOrFunctionName = modernField.classField().getName();
							modernFieldWrapper = modernField.bitField();
							break;
						}
					}
				} else {
					for (var modernFunction : modernClass.functions) {
						if (modernFunction.id() == fieldOrFunction.id) {
							fieldOrFunctionName = modernFunction.classMethod().getName();
							modernFieldWrapper = modernFunction.bitField();
							break;
						}
					}
				}
			}

			if (fieldOrFunctionName == null) {
				if (isField) fieldOrFunctionName = "field " + fieldOrFunction.id;
				else fieldOrFunctionName = "function " + fieldOrFunction.id;
			}

			try {
				hasValues[fieldOrFunction.id] = true;
				if (ReadHelper.readOptional(deserializer.input, fieldOrFunction.bitField.field.optional)) {
					continue;
				}

				if (fieldOrFunction.bitField instanceof ReferenceFieldWrapper) {
					deserializer.structReferenceJobs.add(new BackReadStructReferenceJob(
							values, fieldOrFunction.id, (ReferenceFieldWrapper) fieldOrFunction.bitField,
							new RecursionNode(node, fieldOrFunctionName)
					));
				} else {
					deserializer.input.pushContext(node, fieldOrFunctionName);
					Object value = fieldOrFunction.bitField.read(new BackReadParameters(
							deserializer, modernFieldWrapper, node, fieldOrFunctionName
					));
					deserializer.input.popContext(node, fieldOrFunctionName);

					values[fieldOrFunction.id] = value;
					if (fieldOrFunction.bitField instanceof UUIDFieldWrapper &&
							((UUIDFieldWrapper) fieldOrFunction.bitField).isStableReferenceId
					) {
						legacyObject.stableID = ((LegacyUUIDValue) value).value();
					}

					if (fieldOrFunction.bitField.field.referenceTargetLabel != null) {
						deserializer.references.registerLegacyTarget(
								fieldOrFunction.bitField.field.referenceTargetLabel, value
						);
					}
				}
			} catch (Throwable failed) {
				throw new RecursionException(node.generateTrace(fieldOrFunctionName), failed);
			}
		}
	}

	void read(BackDeserializer deserializer) {
		BitStructWrapper<?> modernWrapper = null;
		if (modernStruct != null) {
			modernWrapper = deserializer.bitser.cache.getWrapperOrNull(modernStruct);
		}

		for (int hierarchyIndex = 0; hierarchyIndex < legacyInfo.classHierarchy.size(); hierarchyIndex++) {
			LegacyClass legacyClass = legacyInfo.classHierarchy.get(hierarchyIndex);
			LegacyClassValues legacyClassInstance = legacyObject.hierarchy[hierarchyIndex];

			SingleClassWrapper modernClassWrapper = null;
			if (modernWrapper != null && hierarchyIndex < modernWrapper.classHierarchy.size()) {
				modernClassWrapper = modernWrapper.classHierarchy.get(hierarchyIndex);
			}

			readFieldsOrFunctions(
					deserializer, legacyClass.fields, true,
					legacyClassInstance.hasValues, legacyClassInstance.values, modernClassWrapper
			);
			readFieldsOrFunctions(
					deserializer, legacyClass.functions, false,
					legacyClassInstance.hasValues, legacyClassInstance.values, modernClassWrapper
			);
		}
	}
}
