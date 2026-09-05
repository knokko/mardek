package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.RecursionException;
import com.github.knokko.bitser.legacy.LegacyStructInstance;

import java.util.Map;

record BackPopulateStructJob(
		BitStructWrapper<?> modernWrapper, Object modernObject, Map<Class<?>, Object[]> allModernValues,
		LegacyStructInstance legacyObject, RecursionNode node
) {

	void populate() {
		for (int hierarchyIndex = 0; hierarchyIndex < modernWrapper.classHierarchy.size(); hierarchyIndex++) {
			var modernClass = modernWrapper.classHierarchy.get(hierarchyIndex);
			var convertedValues = allModernValues.get(modernClass.myClass);
			var legacyValues = legacyObject.hierarchy[hierarchyIndex];
			for (var modernField : modernClass.fields) {
				if (modernField.id() < legacyValues.hasValues.length && legacyValues.hasValues[modernField.id()]) {
					Object modernValue = convertedValues[modernField.id()];
					try {
						modernField.classField().set(modernObject, modernValue);
					} catch (Throwable failed) {
						throw new RecursionException(node.generateTrace(modernField.classField().getName()), failed);
					}
				}
			}
		}
	}
}
