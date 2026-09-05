package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.StableReferenceFieldId;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class BitserCache {

	private final Map<Class<?>, BitStructWrapper<?>> wrappers;
	private final Map<Class<?>, Boolean> hasStableIdMap;

	BitserCache() {
		wrappers = new ConcurrentHashMap<>();
		hasStableIdMap = new ConcurrentHashMap<>();
	}

	<T> BitStructWrapper<T> getWrapper(Class<T> objectClass) {
		BitStructWrapper<T> result = getWrapperOrNull(objectClass);
		if (result == null) {
			throw new InvalidBitFieldException(objectClass + " is not a BitStruct");
		}
		return result;
	}

	void requireStableID(Class<?> objectClass) {
		BitStructWrapper<?> maybeWrapper = getWrapperOrNull(objectClass);
		if (maybeWrapper != null) {
			if (!maybeWrapper.hasStableId()) {
				throw new InvalidBitFieldException(
						objectClass.getSimpleName() + " doesn't have an @StableReferenceFieldId"
				);
			}
		} else {
			if (!hasStableIdMap.computeIfAbsent(objectClass, this::hasStableID)) {
				throw new InvalidBitFieldException(
						objectClass.getSimpleName() + " doesn't have an @StableReferenceFieldId"
				);
			}
		}
	}

	private boolean hasStableID(Class<?> objectClass) {
		Class<?> nextClass = objectClass;
		while (nextClass != null) {
			for (Field field : nextClass.getDeclaredFields()) {
				if (field.getType() == UUID.class && field.isAnnotationPresent(StableReferenceFieldId.class)) {
					return true;
				}
			}
			nextClass = nextClass.getSuperclass();
		}
		return false;
	}

	<T> BitStructWrapper<T> getWrapperOrNull(Class<T> objectClass) {
		//noinspection unchecked
		return (BitStructWrapper<T>) wrappers.computeIfAbsent(objectClass, BitStructWrapper::wrap);
	}
}
