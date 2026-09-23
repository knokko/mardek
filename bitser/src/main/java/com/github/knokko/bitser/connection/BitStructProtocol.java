package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class BitStructProtocol {

	public enum FieldType {
		SIMPLE,
		STRUCT,
		COLLECTION,
		REFERENCE
	}

	@FunctionalInterface
	public interface Serializer {

		void serialize(BitOutputStream output, Object value) throws Throwable;
	}

	@FunctionalInterface
	public interface Deserializer {

		Object deserialize(BitInputStream input) throws Throwable;
	}

	private record FieldKey(Class<?> declaringClass, String fieldName) {}

	private record FieldInfo(
			int id, FieldType type,
			Serializer flatSerializer, Deserializer flatDeserializer,
			Function<Object, Object> getFieldValue,
			BiConsumer<Object, Object> setFieldValue
	) {}

	private final Bitser bitser;
	private final Map<FieldKey, FieldInfo> mapping = new HashMap<>();
	private FieldInfo[] idToInfo;

	private int numFields;
	private boolean finishedRegistration;

	public BitStructProtocol(Bitser bitser) {
		this.bitser = bitser;
	}

	public void addField(Field field, FieldType type, Serializer flatSerializer, Deserializer flatDeserializer) {
		if (finishedRegistration) throw new IllegalStateException("Registration is finished");

		int fieldID = numFields;
		FieldInfo fieldInfo = new FieldInfo(
				fieldID, type, flatSerializer, flatDeserializer,
				structInstance -> {
					try {
						return field.get(structInstance);
					} catch (Throwable failed) {
						throw new RuntimeException(failed);
					}
				},
				(structInstance, newValue) -> {
					try {
						field.set(structInstance, newValue);
					} catch (Throwable failed) {
						throw new RuntimeException(failed);
					}
				}
		);
		mapping.put(new FieldKey(field.getDeclaringClass(), field.getName()), fieldInfo);

		// If exactly one field has this name, the null key can be used.
		// If multiple fields have the same name, the null key is forbidden for that field name.
		FieldKey nullKey = new FieldKey(null, field.getName());
		if (mapping.containsKey(nullKey)) {
			mapping.put(nullKey, null);
		} else {
			mapping.put(nullKey, fieldInfo);
		}

		numFields += 1;
	}

	public void finishRegistration() {
		finishedRegistration = true;
		idToInfo = new FieldInfo[numFields];
		mapping.values().forEach(info -> idToInfo[info.id] = info);
	}

	public int getNumFields() {
		if (!finishedRegistration) throw new IllegalStateException("Registration is still open");
		return numFields;
	}

	public int getFieldId(Class<?> declaringClass, String fieldName) {
		if (!finishedRegistration) throw new IllegalStateException("Registration is still open");

		FieldKey key = new FieldKey(declaringClass, fieldName);
		FieldInfo fieldInfo = mapping.get(key);
		if (fieldInfo != null) return fieldInfo.id;

		if (mapping.containsKey(key)) {
			throw new UnsupportedOperationException("declaringClass must be non-null for ambiguous field " + fieldName);
		} else {
			throw new IllegalArgumentException("Unknown field " + fieldName);
		}
	}

	public Object getFieldValue(Object structInstance, int fieldID) {
		return idToInfo[fieldID].getFieldValue.apply(structInstance);
	}

	public void setFieldValue(Object structInstance, int fieldID, Object newValue) {
		idToInfo[fieldID].setFieldValue.accept(structInstance, newValue);
	}

	FieldType getFieldType(int fieldID) {
		return idToInfo[fieldID].type;
	}

	void serializeFlatFieldValue(int fieldID, BitOutputStream output, Object value) throws Throwable {
		idToInfo[fieldID].flatSerializer.serialize(output, value);
	}

	Object deserializeFlatFieldValue(int fieldID, BitInputStream input) throws Throwable {
		return idToInfo[fieldID].flatDeserializer.deserialize(input);
	}

	boolean areFlatValuesEqual(int fieldID, Object a, Object b) {
		var fieldInfo = idToInfo[fieldID];
		return switch (fieldInfo.type) {
			case SIMPLE -> Objects.equals(a, b);
			case STRUCT -> bitser.deepEquals(a, b);
			default -> throw new IllegalArgumentException("Can't compare 'flat' " + a + " with " + b);
		};
	}
}
