package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.ReferenceFieldTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Ironically, I don't think I can make this backward-compatible
@BitStruct(backwardCompatible = false)
class LegacyClasses {

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@ReferenceFieldTarget(label = "classes")
	private final ArrayList<LegacyClass> classes = new ArrayList<>();

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	@ReferenceFieldTarget(label = "structs")
	private final ArrayList<LegacyStruct> structs = new ArrayList<>();

	private final Map<Class<?>, LegacyClass> classMap = new HashMap<>();
	private final Map<Class<?>, LegacyStruct> structMap = new HashMap<>();

	LegacyStruct getRoot() {
		return structs.get(0);
	}

	LegacyClass getClass(Class<?> javaClass) {
		return classMap.get(javaClass);
	}

	LegacyClass addClass(Class<?> javaClass) {
		if (classMap.containsKey(javaClass)) {
			throw new UnexpectedBitserException("Class " + javaClass + " was already registered");
		}

		LegacyClass legacyClass = new LegacyClass();
		classes.add(legacyClass);
		classMap.put(javaClass, legacyClass);
		return legacyClass;
	}

	LegacyStruct addStruct(Class<?> javaClass) {
		if (structMap.containsKey(javaClass)) {
			throw new UnexpectedBitserException("Struct class " + javaClass + " was already registered");
		}

		LegacyStruct legacyStruct = new LegacyStruct();
		structs.add(legacyStruct);
		structMap.put(javaClass, legacyStruct);
		return legacyStruct;
	}

	LegacyStruct getStruct(Class<?> javaClass) {
		return structMap.get(javaClass);
	}
}
