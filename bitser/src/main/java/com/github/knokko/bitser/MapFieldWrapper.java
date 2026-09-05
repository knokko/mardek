package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyMapValue;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.ClassField;
import com.github.knokko.bitser.field.IntegerField;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.knokko.bitser.AbstractCollectionFieldWrapper.constructCollectionWithSize;
import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class MapFieldWrapper extends BitFieldWrapper {

	@BitField
	private final IntegerField.Properties sizeField;

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper keysWrapper;

	@ClassField(root = BitFieldWrapper.class)
	private final BitFieldWrapper valuesWrapper;

	MapFieldWrapper(VirtualField field, IntegerField sizeField, BitFieldWrapper keysWrapper, BitFieldWrapper valuesWrapper) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) {
			throw new InvalidBitFieldException("Invalid sizeField bounds");
		}
		if (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers())) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = new IntegerField.Properties(
				max(0, sizeField.minValue()), min(Integer.MAX_VALUE, sizeField.maxValue()),
				sizeField.expectUniform(), sizeField.digitSize(), sizeField.commonValues()
		);
		this.keysWrapper = keysWrapper;
		this.valuesWrapper = valuesWrapper;
	}

	@SuppressWarnings("unused")
	private MapFieldWrapper() {
		super();
		this.sizeField = new IntegerField.Properties();
		this.keysWrapper = null;
		this.valuesWrapper = null;
	}

	@Override
	void registerLegacyClasses(UsedStructCollector collector) {
		keysWrapper.registerLegacyClasses(collector);
		valuesWrapper.registerLegacyClasses(collector);
	}

	@Override
	void registerReferenceTargets(
			AbstractReferenceTracker references, Object value,
			RecursionNode parentNode, String fieldName
	) {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		Map<?, ?> map = (Map<?, ?>) value;
		references.arrayJobs.add(new WithArrayJob(map.keySet().toArray(), keysWrapper, childNode));
		references.arrayJobs.add(new WithArrayJob(map.values().toArray(), valuesWrapper, childNode));
	}

	@Override
	public void write(Serializer serializer, Object value, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		Map<?, ?> map = (Map<?, ?>) value;

		if (serializer.intDistribution != null) {
			serializer.intDistribution.insert(field + " map size", (long) map.size(), sizeField);
			serializer.intDistribution.insert("map size", (long) map.size(), sizeField);
		}

		serializer.output.prepareProperty("map-size");
		IntegerBitser.encodeInteger(map.size(), sizeField, serializer.output);
		serializer.output.finishProperty();

		if (map.isEmpty()) return;
		if (keysWrapper instanceof ReferenceFieldWrapper) {
			serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
					map.keySet().toArray(), (ReferenceFieldWrapper) keysWrapper,
					"this map must not contain null keys", childNode
			));
		} else {
			serializer.arrayJobs.add(new WriteArrayJob(
					map.keySet().toArray(), keysWrapper, childNode, "this map must not have null keys"
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			serializer.arrayReferenceJobs.add(new WriteArrayReferenceJob(
					map.values().toArray(), (ReferenceFieldWrapper) valuesWrapper,
					"this map must not contain null values", childNode
			));
		} else {
			serializer.arrayJobs.add(new WriteArrayJob(
					map.values().toArray(), valuesWrapper, childNode, "this map must not have null values"
			));
		}
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		deserializer.input.prepareProperty("map-size");
		int size = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "map-size", deserializer.input);
		deserializer.input.finishProperty();

		Object[] keys = new Object[size];
		Object[] values = new Object[size];
		Map<?, ?> map = (Map<?, ?>) constructCollectionWithSize(field.type, keysWrapper.field.type, size);
		if (size == 0) return map;

		if (keysWrapper instanceof ReferenceFieldWrapper) {
			deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
					keys, (ReferenceFieldWrapper) keysWrapper, childNode
			));
		} else {
			deserializer.arrayJobs.add(new ReadArrayJob(
					keys, keysWrapper, childNode
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			deserializer.arrayReferenceJobs.add(new ReadArrayReferenceJob(
					values, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			deserializer.arrayJobs.add(new ReadArrayJob(
					values, valuesWrapper, childNode
			));
		}
		deserializer.populateJobs.add(new PopulateMapJob(map, keys, values, childNode));

		return map;
	}

	@Override
	public Object read(BackReadParameters parameters) throws Throwable {
		RecursionNode childNode = new RecursionNode(parameters.parentNode(), parameters.fieldName());
		parameters.deserializer().input.prepareProperty("map-size");
		int size = IntegerBitser.decodeLength(
				sizeField, parameters.deserializer().sizeLimit,
				"map-size", parameters.deserializer().input
		);
		parameters.deserializer().input.finishProperty();

		Object[] keys = new Object[size];
		Object[] values = new Object[size];
		if (size == 0) return new LegacyMapValue(keys, values);

		BitFieldWrapper modernKeysWrapper = null;
		BitFieldWrapper modernValuesWrapper = null;
		if (parameters.modernField() instanceof MapFieldWrapper modernMap) {
			modernKeysWrapper = modernMap.keysWrapper;
			modernValuesWrapper = modernMap.valuesWrapper;
		}
		if (keysWrapper instanceof ReferenceFieldWrapper) {
			parameters.deserializer().arrayReferenceJobs.add(new BackReadArrayReferenceJob(
					keys, (ReferenceFieldWrapper) keysWrapper, childNode
			));
		} else {
			parameters.deserializer().arrayJobs.add(new BackReadArrayJob(
					keys, keysWrapper, modernKeysWrapper, childNode
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			parameters.deserializer().arrayReferenceJobs.add(new BackReadArrayReferenceJob(
					values, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			parameters.deserializer().arrayJobs.add(new BackReadArrayJob(
					values, valuesWrapper, modernValuesWrapper, childNode
			));
		}

		return new LegacyMapValue(keys, values);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (!(legacyValue instanceof LegacyMapValue legacyMap)) {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue +
					" to map for field " + field);
		}
		int size = legacyMap.keys().length;

		Map<?, ?> modernMap = (Map<?, ?>) constructCollectionWithSize(field.type, keysWrapper.field.type, size);
		Object modernKeys = Array.newInstance(keysWrapper.field.type, size);
		Object modernValues = Array.newInstance(valuesWrapper.field.type, size);

		RecursionNode childNode = new RecursionNode(parentNode, fieldName);
		if (keysWrapper instanceof ReferenceFieldWrapper) {
			deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
					legacyMap.keys(), modernKeys, (ReferenceFieldWrapper) keysWrapper, childNode
			));
		} else {
			deserializer.convertArrayJobs.add(new BackConvertArrayJob(
					legacyMap.keys(), modernKeys, keysWrapper, childNode
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			deserializer.convertArrayReferenceJobs.add(new BackConvertArrayReferenceJob(
					legacyMap.values(), modernValues, (ReferenceFieldWrapper) valuesWrapper, childNode
			));
		} else {
			deserializer.convertArrayJobs.add(new BackConvertArrayJob(
					legacyMap.values(), modernValues, valuesWrapper, childNode
			));
		}

		deserializer.populateJobs.add(new PopulateMapJob(
				modernMap, (Object[]) modernKeys, (Object[]) modernValues, childNode)
		);
		return modernMap;
	}

	@Override
	Object deepCopy(
			Object original, DeepCopyMachine machine,
			RecursionNode parentNode, String fieldName
	) {
		if (original == null) return null;

		var originalMap = (Map<?, ?>)  original;
		var childNode = new RecursionNode(parentNode, fieldName);
		Object[] sourceKeys = originalMap.keySet().toArray();
		Object[] sourceValues = originalMap.values().toArray();
		Object[] destinationKeys = new Object[sourceKeys.length];
		Object[] destinationValues = new Object[destinationKeys.length];

		if (keysWrapper instanceof ReferenceFieldWrapper) {
			machine.arrayReferenceJobs.add(new DeepCopyArrayReferenceJob(
					destinationKeys, sourceKeys, childNode
			));
		} else {
			machine.arrayJobs.add(new DeepCopyArrayJob(
					sourceKeys, destinationKeys, keysWrapper, childNode, "keys"
			));
		}

		if (valuesWrapper instanceof ReferenceFieldWrapper) {
			machine.arrayReferenceJobs.add(new DeepCopyArrayReferenceJob(
					destinationValues, sourceValues, childNode
			));
		} else {
			machine.arrayJobs.add(new DeepCopyArrayJob(
					sourceValues, destinationValues, valuesWrapper, childNode, "values"
			));
		}

		var newMap = (Map<?, ?>) constructCollectionWithSize(
				field.type, keysWrapper.field.type, sourceKeys.length
		);
		machine.populateJobs.add(new PopulateMapJob(newMap, destinationKeys, destinationValues, childNode));
		return newMap;
	}

	@Override
	void collectInstances(InstanceCollector collector, Object value, RecursionNode parentNode, String fieldName) {
		var childNode = new RecursionNode(parentNode, fieldName);
		var map = (Map<?, ?>) value;
		collector.arrayJobs.add(new CollectFromArrayJob(
				map.keySet().toArray(), keysWrapper, childNode, "keys"
		));
		collector.arrayJobs.add(new CollectFromArrayJob(
				map.values().toArray(), valuesWrapper, childNode, "values"
		));
	}

	@Override
	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		if (value != null) {
			var map = (Map<?, ?>) value;
			computer.arrayJobs.add(new HashArrayJob(
					map.keySet().toArray(), keysWrapper, new RecursionNode(parentNode, fieldName), "keys")
			);
			computer.arrayJobs.add(new HashArrayJob(
					map.values().toArray(), valuesWrapper, new RecursionNode(parentNode, fieldName), "values")
			);
		} else computer.digest.update((byte) -15);
	}

	@Override
	boolean certainlyNotEqual(
			DeepComparator comparator, Object valueA, Object valueB,
			RecursionNode node, String fieldName
	) {
		var mapA = (Map<?, ?>) valueA;
		var mapB = (Map<?, ?>) valueB;
		var childNode = new RecursionNode(node, fieldName);
		comparator.arrayJobs.add(new DeepCompareArraysJob(
				mapA.keySet().toArray(), mapB.keySet().toArray(), keysWrapper, childNode, "keys"
		));
		comparator.arrayJobs.add(new DeepCompareArraysJob(
				mapA.values().toArray(), mapB.values().toArray(), valuesWrapper, childNode, "values"
		));
		return false;
	}
}
