package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyArrayValue;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;

import java.lang.reflect.Array;
import java.util.*;

@BitStruct(backwardCompatible = false)
class BitCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper valuesWrapper;

	BitCollectionFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper valuesWrapper) {
		super(field, sizeField);
		this.valuesWrapper = valuesWrapper;
	}

	@SuppressWarnings("unused")
	private BitCollectionFieldWrapper() {
		super();
		this.valuesWrapper = null;
	}

	@Override
	ArrayType determineArrayType() {
		return null;
	}

	@Override
	void registerLegacyClasses(UsedStructCollector collector) {
		valuesWrapper.registerLegacyClasses(collector);
	}

	@Override
	void registerReferenceTargets(
			AbstractReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			references.arrayJobs.add(new WithArrayJob(value, valuesWrapper, childNode));
		} else {
			references.arrayJobs.add(new WithArrayJob(((Collection<?>) value).toArray(), valuesWrapper, childNode));
		}
	}

	@Override
	public void write(Serializer serializer, Object value, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			int length = Array.getLength(value);
			if (serializer.intDistribution != null) {
				serializer.intDistribution.insert(field + " collection size", (long) length, sizeField);
				serializer.intDistribution.insert("collection size", (long) length, sizeField);
			}

			serializer.output.prepareProperty("array-length");
			IntegerBitser.encodeInteger(length, sizeField, serializer.output);
			serializer.output.finishProperty();

			if (length == 0) return;
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
						value, (ReferenceFieldWrapper) valuesWrapper,
						"this array must not have null elements", childNode
				));
			} else {
				serializer.arrayJobs.add(new WriteArrayJob(
						value, valuesWrapper, childNode, "this array must not have null elements"
				));
			}
		} else {
			Collection<?> collection = (Collection<?>) value;
			if (serializer.intDistribution != null) {
				serializer.intDistribution.insert(field + " collection size", (long) collection.size(), sizeField);
				serializer.intDistribution.insert("collection size", (long) collection.size(), sizeField);
			}

			serializer.output.prepareProperty("collection-size");
			IntegerBitser.encodeInteger(collection.size(), sizeField, serializer.output);
			serializer.output.finishProperty();

			if (collection.isEmpty()) return;
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
						collection.toArray(), (ReferenceFieldWrapper) valuesWrapper,
						"this collection must not have null elements", childNode
				));
			} else {
				serializer.arrayJobs.add(new WriteArrayJob(
						collection.toArray(), valuesWrapper, childNode,
						"this collection must not have null elements"
				));
			}
		}
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			deserializer.input.prepareProperty("array-length");
			int length = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "array-length", deserializer.input);
			deserializer.input.finishProperty();

			Object array = Array.newInstance(field.type.getComponentType(), length);
			if (length == 0) return array;

			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
						array, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.arrayJobs.add(new ReadArrayJob(
						array, valuesWrapper, childNode
				));
			}

			return array;
		} else {
			deserializer.input.prepareProperty("collection-size");
			int size = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "collection-size", deserializer.input);
			deserializer.input.finishProperty();

			Object[] array = new Object[size];
			Collection<?> collection = (Collection<?>) constructCollectionWithSize(field.type, valuesWrapper.field.type, size);
			if (size == 0) return collection;

			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
						array, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.arrayJobs.add(new ReadArrayJob(
						array, valuesWrapper, childNode
				));
			}
			deserializer.populateJobs.add(new PopulateCollectionJob(collection, array, childNode));

			return collection;
		}
	}

	@Override
	public Object read(BackReadParameters parameters) throws Throwable {
		RecursionNode childNode = new RecursionNode(parameters.parentNode(), parameters.fieldName());
		parameters.deserializer().input.prepareProperty("legacy-array-length");
		int length = IntegerBitser.decodeLength(
				sizeField, parameters.deserializer().sizeLimit,
				"legacy-array-length", parameters.deserializer().input
		);
		parameters.deserializer().input.finishProperty();

		Object array = constructCollectionWithSize(length);
		if (length == 0) return new LegacyArrayValue(array);

		BitFieldWrapper modernElementsWrapper = null;
		if (parameters.modernField() instanceof BitCollectionFieldWrapper modernCollectionField) {
			modernElementsWrapper = modernCollectionField.valuesWrapper;
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			parameters.deserializer().arrayReferenceJobs.add(new BackReadArrayReferenceJob(
					array, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			parameters.deserializer().arrayJobs.add(new BackReadArrayJob(
					array, valuesWrapper, modernElementsWrapper, childNode
			));
		}

		return new LegacyArrayValue(array);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (!(legacyValue instanceof LegacyArrayValue legacyArrayWrapper)) {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue +
					" to collection/array for field " + field);
		}
		Object legacyArray = legacyArrayWrapper.array;
		int length = Array.getLength(legacyArray);

		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (field.type.isArray()) {
			Object modernArray = Array.newInstance(field.type.getComponentType(), length);
			legacyArrayWrapper.modernObject = modernArray;
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
						legacyArray, modernArray, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.convertArrayJobs.add(new BackConvertArrayJob(
						legacyArray, modernArray, valuesWrapper, childNode
				));
			}

			return modernArray;
		} else {
			Object modernArray = Array.newInstance(valuesWrapper.field.type, length);
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
						legacyArray, modernArray, (ReferenceFieldWrapper) valuesWrapper, childNode
				));
			} else {
				deserializer.convertArrayJobs.add(new BackConvertArrayJob(
						legacyArray, modernArray, valuesWrapper, childNode
				));
			}

			Object modernCollection = constructCollectionWithSize(length);
			deserializer.populateJobs.add(new PopulateCollectionJob(
					(Collection<?>) modernCollection, (Object[]) modernArray, childNode)
			);
			legacyArrayWrapper.modernObject = modernCollection;
			return modernCollection;
		}
	}

	@Override
	Object deepCopy(
			Object original, DeepCopyMachine machine,
			RecursionNode parentNode, String fieldName
	) {
		var childNode = new RecursionNode(parentNode, fieldName);
		if (original instanceof Collection<?> originalCollection) {
			Object[] sourceArray = originalCollection.toArray();
			Object[] destinationArray = new Object[sourceArray.length];
			var newCollection = (Collection<?>) constructCollectionWithSize(sourceArray.length);

			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				machine.arrayReferenceJobs.add(new DeepCopyArrayReferenceJob(
						destinationArray, sourceArray, childNode
				));
			} else {
				machine.arrayJobs.add(new DeepCopyArrayJob(
						sourceArray, destinationArray, valuesWrapper, childNode, "elements")
				);
			}

			machine.populateJobs.add(new PopulateCollectionJob(newCollection, destinationArray, childNode));
			return newCollection;
		} else {
			Object destinationArray = constructCollectionWithSize(Array.getLength(original));
			if (valuesWrapper instanceof ReferenceFieldWrapper) {
				machine.arrayReferenceJobs.add(new DeepCopyArrayReferenceJob(
						destinationArray, original, childNode
				));
			} else {
				machine.arrayJobs.add(new DeepCopyArrayJob(
						original, destinationArray, valuesWrapper, childNode, "elements")
				);
			}
			return destinationArray;
		}
	}

	@Override
	void collectInstances(InstanceCollector collector, Object value, RecursionNode parentNode, String fieldName) {
		var childNode = new RecursionNode(parentNode, fieldName);
		if (value instanceof Collection<?> collection) {
			collector.arrayJobs.add(new CollectFromArrayJob(
					collection.toArray(), valuesWrapper, childNode, "elements"
			));
		} else {
			collector.arrayJobs.add(new CollectFromArrayJob(value, valuesWrapper, childNode, "elements"));
		}
	}

	@Override
	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		if (value != null) {
			Object array;
			if (value instanceof Collection<?>) array = ((Collection<?>) value).toArray();
			else array = value;
			computer.arrayJobs.add(new HashArrayJob(
					array, valuesWrapper, new RecursionNode(parentNode, fieldName), "elements")
			);
		} else computer.digest.update((byte) -12);
	}

	@Override
	boolean certainlyNotEqual(
			DeepComparator comparator, Object valueA, Object valueB,
			RecursionNode node, String fieldName
	) {
		Object arrayA, arrayB;
		if (valueA instanceof Collection<?> collectionA) arrayA = collectionA.toArray();
		else arrayA = valueA;
		if (valueB instanceof Collection<?> collectionB) arrayB = collectionB.toArray();
		else arrayB = valueB;

		var childNode = new RecursionNode(node, fieldName);
		comparator.arrayJobs.add(new DeepCompareArraysJob(
				arrayA, arrayB, valuesWrapper, childNode, "elements"
		));
		return false;
	}
}
