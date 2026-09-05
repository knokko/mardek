package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.legacy.LegacyStructInstance;
import com.github.knokko.bitser.legacy.ReferenceLegacyLazyBytes;
import com.github.knokko.bitser.legacy.SimpleLegacyLazyBytes;

@BitStruct(backwardCompatible = false)
class ReferenceLazyFieldWrapper extends BitFieldWrapper {

	private final Class<?> valueClass;

	@BitField(id = 0)
	private final String[] labels;

	@SuppressWarnings("unused")
	private ReferenceLazyFieldWrapper() {
		super();
		this.valueClass = null;
		this.labels = null;
	}

	ReferenceLazyFieldWrapper(VirtualField field, Class<?> valueClass, String[] labels) {
		super(field);
		this.valueClass = valueClass;
		this.labels = labels;
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		BitStructWrapper<?> valueWrapper = serializer.bitser.cache.getWrapper(valueClass);
		serializer.lazyJobs.add(new WriteLazyJob(
				(ReferenceLazyBits<?>) value, valueWrapper, labels,
				new RecursionNode(parentNode, fieldName)
		));
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		var lazy = new ReferenceLazyBits<>(deserializer.bitser, false, valueClass);
		deserializer.lazyJobs.add(new ReadLazyJob(lazy, labels, new RecursionNode(parentNode, fieldName)));
		return lazy;
	}

	@Override
	Object read(BackReadParameters parameters) throws Throwable {
		var lazy = new ReferenceLazyBits<>(parameters.deserializer().bitser, true, null);
		parameters.deserializer().lazyJobs.add(new ReadLazyJob(
				lazy, labels, new RecursionNode(parameters.parentNode(), parameters.fieldName())
		));
		lazy.labels = labels;
		return new ReferenceLegacyLazyBytes(lazy);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof ReferenceLegacyLazyBytes lazyWrapper) {
			lazyWrapper.lazy().valueClass = valueClass;
			return lazyWrapper.lazy();
		}
		if (legacyValue instanceof SimpleLegacyLazyBytes simple) {
			var nonLazy = deserializer.bitser.fromBytes(
					valueClass, simple.bytes(), Bitser.BACKWARD_COMPATIBLE
			);
			return new ReferenceLazyBits<>(nonLazy);
		}
		if (legacyValue instanceof LegacyStructInstance legacyObject) {
			BitStructWrapper<?> modernInfo = deserializer.bitser.cache.getWrapper(valueClass);
			Object modernObject = modernInfo.createEmptyInstance();
			legacyObject.modernObject = modernObject;
			deserializer.convertStructJobs.add(new BackConvertStructJob(
					modernInfo, legacyObject,
					new RecursionNode(parentNode, fieldName)
			));
			return new ReferenceLazyBits<>(modernObject);
		}
		throw new LegacyBitserException(
				"Can't convert from legacy " + legacyValue + " to " + valueClass + " for field " + field
		);
	}

	@Override
	Object deepCopy(
			Object original, DeepCopyMachine machine,
			RecursionNode parentNode, String fieldName
	) {
		var copied = new ReferenceLazyBits<Object>(machine.bitser, false, valueClass);
		BitStructWrapper<?> valueWrapper = machine.bitser.cache.getWrapper(valueClass);
		machine.lazyJobs.add(new DeepCopyLazyJob(
				(ReferenceLazyBits<?>) original, copied, valueWrapper, labels,
				new RecursionNode(parentNode, fieldName)
		));
		return copied;
	}

	@Override
	void collectInstances(InstanceCollector collector, Object value, RecursionNode parentNode, String fieldName) {
		var wrapper = collector.bitser.cache.getWrapper(valueClass);
		var wrappedValue = ((ReferenceLazyBits<?>) value).get();
		collector.register(wrappedValue);
		collector.structJobs.add(new CollectFromStructJob(
				wrappedValue, wrapper, new RecursionNode(parentNode, fieldName))
		);
	}

	@Override
	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		if (value != null) {
			var lazy = (ReferenceLazyBits<?>) value;
			var wrapped = lazy.get();
			computer.structJobs.add(new HashStructJob(
					wrapped, computer.bitser.cache.getWrapper(wrapped.getClass()),
					new RecursionNode(parentNode, fieldName)
			));
		} else computer.digest.update((byte) 37);
	}

	@Override
	boolean certainlyNotEqual(
			DeepComparator comparator, Object valueA, Object valueB,
			RecursionNode node, String fieldName
	) {
		var lazyA = (ReferenceLazyBits<?>) valueA;
		var lazyB = (ReferenceLazyBits<?>) valueB;

		var wrapper = comparator.bitser.cache.getWrapper(valueClass);
		comparator.lazyJobs.add(new DeepCompareLazyJob(lazyA, lazyB, wrapper, new RecursionNode(node, fieldName)));
		return false;
	}
}
