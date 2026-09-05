package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;

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
	final Constructor<T> constructor;
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

		try {
			this.constructor = objectClass.getDeclaredConstructor();
			try {
				constructor.newInstance();
			} catch (IllegalAccessException e) {
				constructor.setAccessible(true);
			} catch (InstantiationException shouldNotHappen) {
				throw new InvalidBitFieldException(
						"Class " + objectClass + " cannot be instantiated: " + shouldNotHappen.getMessage()
				);
			} catch (InvocationTargetException failedConstruction) {
				throw new InvalidBitFieldException(
						"The constructor of " + objectClass + " failed: " + failedConstruction.getMessage()
				);
			}
		} catch (NoSuchMethodException e) {
			throw new InvalidBitFieldException(objectClass + " must have a constructor without parameters");
		}

		this.classHierarchy = new ArrayList<>();
		Class<?> currentClass = objectClass;
		while (currentClass != null) {
			this.classHierarchy.add(new SingleClassWrapper(currentClass, bitStruct.backwardCompatible()));
			currentClass = currentClass.getSuperclass();
		}

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

	T createEmptyInstance() {
		try {
			return constructor.newInstance();
		} catch (InstantiationException e) {
			throw new UnexpectedBitserException("Failed to instantiate " + constructor);
		} catch (IllegalAccessException shouldNotHappen) {
			throw new UnexpectedBitserException("Can't get access to " + constructor);
		} catch (InvocationTargetException e) {
			throw new InvalidBitFieldException("Constructor " + constructor + " throw an exception: " + e.getMessage());
		}
	}

	T shallowCopy(Object original) {
		T copy = createEmptyInstance();
		for (SingleClassWrapper currentClass : classHierarchy) {
			currentClass.shallowCopy(original, copy);
		}
		return copy;
	}
}
