package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.legacy.LegacyFloatValue;
import com.github.knokko.bitser.legacy.LegacyIntValue;

import static com.github.knokko.bitser.FloatBitser.decodeFloat;
import static com.github.knokko.bitser.FloatBitser.encodeFloat;

@BitStruct(backwardCompatible = false)
class FloatFieldWrapper extends BitFieldWrapper {

	@BitField
	private final FloatField.Properties floatField;

	@BitField
	private final boolean isFloat;

	FloatFieldWrapper(VirtualField field, FloatField floatField) {
		super(field);
		this.floatField = new FloatField.Properties(floatField);

		Class<?> type = field.type;
		if (type != float.class && type != double.class && type != Float.class && type != Double.class) {
			throw new InvalidBitFieldException("FloatField only supports floats and doubles, but got " + type);
		}
		this.isFloat = type == float.class || type == Float.class;
	}

	@SuppressWarnings("unused")
	private FloatFieldWrapper() {
		super();
		this.floatField = new FloatField.Properties();
		this.isFloat = false;
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		if (serializer.floatDistribution != null) {
			serializer.floatDistribution.insert(field.toString(), ((Number) value).doubleValue(), floatField);
		}
		encodeFloat(((Number) value).doubleValue(), !isFloat, floatField, serializer.output);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		double value = decodeFloat(!isFloat, floatField, deserializer.input);
		if (isFloat) return (float) value;
		else return value;
	}

	@Override
	Object read(BackReadParameters parameters) throws Throwable {
		double value = decodeFloat(!isFloat, floatField, parameters.deserializer().input);
		return new LegacyFloatValue(value);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof LegacyIntValue) {
			long longValue = ((LegacyIntValue) legacyValue).value();
			if (isFloat) return (float) longValue;
			return (double) longValue;
		} else if (legacyValue instanceof LegacyFloatValue) {
			double doubleValue = ((LegacyFloatValue) legacyValue).value();
			if (isFloat) return (float) doubleValue;
			return doubleValue;
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to " + field.type + " for field " + field);
		}
	}
}
