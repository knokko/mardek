package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.legacy.LegacyIntValue;

import static com.github.knokko.bitser.IntegerBitser.*;
import static java.lang.Long.max;
import static java.lang.Long.min;

@BitStruct(backwardCompatible = false)
class IntegerFieldWrapper extends BitFieldWrapper {

	private static long getMinValue(long rawMinValue, Class<?> type) {
		long classMinValue = Long.MIN_VALUE;

		if (type == byte.class || type == Byte.class) classMinValue = Byte.MIN_VALUE;
		if (type == short.class || type == Short.class) classMinValue = Short.MIN_VALUE;
		if (type == int.class || type == Integer.class) classMinValue = Integer.MIN_VALUE;

		return max(rawMinValue, classMinValue);
	}

	private static long getMaxValue(long rawMaxValue, Class<?> type) {
		long classMaxValue = Long.MAX_VALUE;

		if (type == byte.class || type == Byte.class) classMaxValue = Byte.MAX_VALUE;
		if (type == short.class || type == Short.class) classMaxValue = Short.MAX_VALUE;
		if (type == int.class || type == Integer.class) classMaxValue = Integer.MAX_VALUE;

		return min(rawMaxValue, classMaxValue);
	}

	@BitField
	private final IntegerField.Properties intField;

	IntegerFieldWrapper(VirtualField field, IntegerField intField) {
		super(field);
		this.intField = new IntegerField.Properties(
				getMinValue(intField.minValue(), field.type),
				getMaxValue(intField.maxValue(), field.type),
				intField.expectUniform(),
				intField.digitSize(),
				intField.commonValues()
		);
	}

	@SuppressWarnings("unused")
	private IntegerFieldWrapper() {
		super();
		this.intField = new IntegerField.Properties();
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		long longValue = value instanceof Character ? (long) ((char) value) : ((Number) value).longValue();
		if (serializer.intDistribution != null) {
			serializer.intDistribution.insert(field.toString(), longValue, intField);
		}
		serializer.output.prepareProperty("int-value");
		encodeInteger(longValue, intField, serializer.output);
		serializer.output.finishProperty();
	}

	private Object toRightType(long longValue) {
		Class<?> type = field.type;
		if (type == byte.class || type == Byte.class) return (byte) longValue;
		else if (type == short.class || type == Short.class) return (short) longValue;
		else if (type == char.class || type == Character.class) return (char) longValue;
		else if (type == int.class || type == Integer.class) return (int) longValue;
		else if (type == long.class || type == Long.class || type == null) return longValue;
		else throw new InvalidBitFieldException("Unexpected integer type " + type);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("int-value");
		long longValue = decodeInteger(intField, deserializer.input);
		deserializer.input.finishProperty();
		return toRightType(longValue);
	}

	@Override
	Object read(BackReadParameters parameters) throws Throwable {
		parameters.deserializer().input.prepareProperty("int-value");
		long longValue = decodeInteger(intField, parameters.deserializer().input);
		parameters.deserializer().input.finishProperty();
		return new LegacyIntValue(longValue);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof LegacyIntValue) {
			long longValue = ((LegacyIntValue) legacyValue).value();
			if (longValue < intField.minValue || longValue > intField.maxValue) {
				throw new LegacyBitserException("Legacy value " + longValue + " is out of range for field " + field);
			}
			return toRightType(longValue);
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to integer for field " + field);
		}
	}
}
