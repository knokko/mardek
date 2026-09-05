package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;

import java.lang.reflect.*;
import java.util.*;

class BitStructWrapper<T> {

	static <T> BitStructWrapper<T> wrap(Class<T> objectClass) {
		BitStruct bitStruct = objectClass.getAnnotation(BitStruct.class);
		if (bitStruct != null) return new BitStructWrapper<>(objectClass, bitStruct);

		return null;
	}

	private final BitStruct bitStruct;
	final List<SingleClassWrapper> classHierarchy;
	final ObjectFactory<T> mutator;
	private final VirtualField stableIdField;

	BitStructWrapper(Class<T> objectClass, BitStruct bitStruct) {
		if (bitStruct == null) {
			throw new InvalidBitFieldException("Class must have a BitStruct annotation: " + objectClass);
		}
		this.bitStruct = bitStruct;

		if (Modifier.isAbstract(objectClass.getModifiers())) {
			throw new InvalidBitFieldException(objectClass + " is abstract");
		}
		if (Modifier.isInterface(objectClass.getModifiers())) {
			throw new InvalidBitFieldException(objectClass + " is an interface");
		}

		this.classHierarchy = new ArrayList<>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			SingleClassWrapper nextClass = new SingleClassWrapper(currentClass, bitStruct.backwardCompatible());
			this.classHierarchy.add(nextClass);
			currentClass = currentClass.getSuperclass();
		}

		this.mutator = new ObjectFactory<>(objectClass);
		this.stableIdField = findStableField(objectClass);
	}

	private VirtualField findStableField(Class<T> objectClass) {
		VirtualField stableIdField = null;

		for (SingleClassWrapper currentClass : classHierarchy) {
			for (SingleClassWrapper.FieldWrapper field : currentClass.fields) {
				if (field.bitField() instanceof UUIDFieldWrapper && ((UUIDFieldWrapper) field.bitField()).isStableReferenceId) {
					if (stableIdField != null) throw new InvalidBitFieldException(
							"Bit struct " + objectClass + " has multiple stable ID fields, but at most 1 is allowed"
					);
					stableIdField = field.bitField().field;
				}
			}
		}

		return stableIdField;
	}

	void assertBackwardCompatible() {
		if (!this.bitStruct.backwardCompatible()) {
			throw new InvalidBitFieldException("BitStruct " + classHierarchy.get(0) + " is not backward compatible");
		}
	}

	boolean hasStableId() {
		return stableIdField != null;
	}

	UUID getStableId(Object target) {
		if (stableIdField == null) throw new InvalidBitFieldException(target + " doesn't have an @StableReferenceFieldId");
		return (UUID) stableIdField.getValue.apply(target);
	}

	T shallowCopy(Object original) {
		T copy = mutator.createEmptyInstance.get();
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.shallowCopy(original, copy);
		}
		return copy;
	}
}
