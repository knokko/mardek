package com.github.knokko.bitser;

import java.util.ArrayList;
import java.util.HashSet;

class UsedStructCollector {

	private final BitserCache cache;
	private final LegacyClasses destination;

	private final HashSet<Class<?>> encounteredStructs = new HashSet<>();
	private final ArrayList<Class<?>> structsToCollect = new ArrayList<>();

	UsedStructCollector(BitserCache cache, LegacyClasses destination, Class<?> rootStructClass) {
		this.cache = cache;
		this.destination = destination;
		maybeRegisterStruct(rootStructClass);
	}

	void maybeRegisterStruct(Class<?> structClass) {
		if (encounteredStructs.add(structClass)) structsToCollect.add(structClass);
	}

	void collect() {
		while (!structsToCollect.isEmpty()) {
			Class<?> structClass = structsToCollect.remove(structsToCollect.size() - 1);
			BitStructWrapper<?> structInfo = cache.getWrapper(structClass);
			structInfo.assertBackwardCompatible();
			LegacyStruct newStruct = destination.addStruct(structClass);

			for (int hierarchyIndex = 0; hierarchyIndex < structInfo.classHierarchy.size(); hierarchyIndex++) {
				SingleClassWrapper currentClass = structInfo.classHierarchy.get(hierarchyIndex);
				LegacyClass existingClass = destination.getClass(currentClass.myClass);
				if (existingClass != null) {
					newStruct.classHierarchy.add(existingClass);
					continue;
				}

				LegacyClass newClass = destination.addClass(currentClass.myClass);
				newStruct.classHierarchy.add(newClass);

				for (int index = 0; index < currentClass.fieldsSortedById.size(); index++) {
					SingleClassWrapper.FieldWrapper field = currentClass.fieldsSortedById.get(index);
					newClass.fields.add(new LegacyField(field.id(), field.bitField(), field.readsMethodResult()));
					field.bitField().registerLegacyClasses(this);
				}

				for (int index = 0; index < currentClass.functions.size(); index++) {
					SingleClassWrapper.FunctionWrapper function = currentClass.functions.get(index);
					newClass.functions.add(new LegacyField(function.id(), function.bitField(), false));
					function.bitField().registerLegacyClasses(this);
				}
			}
		}
	}
}
