package com.github.knokko.bitser;

import com.github.knokko.bitser.legacy.LegacyClassValues;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.field.ReferenceField;

import java.io.Serializable;
import java.util.ArrayList;

@BitStruct(backwardCompatible = false)
class LegacyStruct implements Serializable {

	@ReferenceField(stable = false, label = "classes")
	final ArrayList<LegacyClass> classHierarchy = new ArrayList<>();

	LegacyStructInstance constructEmptyInstance(int allowedClassIndex) {
		LegacyClassValues[] classes = new LegacyClassValues[classHierarchy.size()];
		for (int index = 0; index < classHierarchy.size(); index++) {
			classes[index] = classHierarchy.get(index).constructEmptyInstance();
		}
		return new LegacyStructInstance(allowedClassIndex, classes);
	}
}
