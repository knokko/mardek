package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;

import java.util.Objects;

abstract class BitFieldWrapper {

	@SuppressWarnings("unused")
	private static final Class<?>[] BITSER_HIERARCHY = {
			BooleanFieldWrapper.class, IntegerFieldWrapper.class, FloatFieldWrapper.class,
			StringFieldWrapper.class, UUIDFieldWrapper.class, EnumFieldWrapper.class,
			BitCollectionFieldWrapper.class, ByteCollectionFieldWrapper.class, MapFieldWrapper.class,
			StructFieldWrapper.class, SimpleLazyFieldWrapper.class, ReferenceLazyFieldWrapper.class,
			ReferenceFieldWrapper.class,
	};

	@BitField
	final VirtualField field;

	BitFieldWrapper(VirtualField field) {
		this.field = field;
		if (field.optional && field.type.isPrimitive()) {
			throw new InvalidBitFieldException("Primitive field " + field + " can't be optional");
		}
	}

	BitFieldWrapper() {
		this.field = new VirtualField();
	}

	void registerLegacyClasses(UsedStructCollector collector) {}

	void registerReferenceTargets(
			AbstractReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {}

	abstract void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable;

	abstract Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable;

	abstract Object read(BackReadParameters parameters) throws Throwable;

	abstract Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName);

	Object deepCopy(
			Object original, DeepCopyMachine machine,
			RecursionNode parentNode, String fieldName
	) {
		return original;
	}

	void collectInstances(InstanceCollector collector, Object value, RecursionNode parentNode, String fieldName) {}

	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		int hashCode = Objects.hashCode(value);
		computer.digest.update((byte) hashCode);
		computer.digest.update((byte) (hashCode >> 8));
		computer.digest.update((byte) (hashCode >> 16));
		computer.digest.update((byte) (hashCode >> 24));
	}

	boolean certainlyNotEqual(
			DeepComparator comparator, Object valueA, Object valueB,
			RecursionNode node, String fieldName
	) {
		return !valueA.equals(valueB);
	}
}
