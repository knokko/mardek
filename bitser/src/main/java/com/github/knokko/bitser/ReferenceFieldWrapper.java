package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.BitField;

@BitStruct(backwardCompatible = false)
class ReferenceFieldWrapper extends BitFieldWrapper {

	@BitField
	final String label;

	@BitField
	final boolean stable;

	ReferenceFieldWrapper(VirtualField field, String label, boolean stable) {
		super(field);
		this.label = label;
		this.stable = stable;
	}

	@SuppressWarnings("unused")
	private ReferenceFieldWrapper() {
		super();
		this.label = "";
		this.stable = false;
	}

	@Override
	void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	Object read(BackReadParameters parameters) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	Object deepCopy(
			Object original, DeepCopyMachine machine,
			RecursionNode parentNode, String fieldName
	) {
		throw new UnexpectedBitserException("Reference fields should get special treatment");
	}

	@Override
	void collectInstances(InstanceCollector collector, Object value, RecursionNode parentNode, String fieldName) {
		throw new UnexpectedBitserException("Reference fields should be skipped");
	}

	@Override
	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		computer.digest.update(value == null ? (byte) 41 : (stable ? (byte) 40 : (byte) 45));
	}

	@Override
	boolean certainlyNotEqual(
			DeepComparator comparator, Object valueA, Object valueB,
			RecursionNode node, String fieldName
	) {
		comparator.referenceJobs.add(new DeepCompareReferenceJob(valueA, valueB));
		return false;
	}
}
