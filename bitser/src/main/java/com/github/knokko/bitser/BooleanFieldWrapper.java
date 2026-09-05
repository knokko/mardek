package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.LegacyBooleanValue;

@BitStruct(backwardCompatible = false)
class BooleanFieldWrapper extends BitFieldWrapper {

	BooleanFieldWrapper(VirtualField field) {
		super(field);
	}

	@SuppressWarnings("unused")
	private BooleanFieldWrapper() {
		super();
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		serializer.output.prepareProperty("boolean-value");
		serializer.output.write((Boolean) value);
		serializer.output.finishProperty();
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("boolean-value");
		boolean result = deserializer.input.read();
		deserializer.input.finishProperty();
		return result;
	}

	@Override
	Object read(BackReadParameters parameters) throws Throwable {
		parameters.deserializer().input.prepareProperty("boolean-value");
		boolean result = parameters.deserializer().input.read();
		parameters.deserializer().input.finishProperty();
		return result ? LegacyBooleanValue.TRUE : LegacyBooleanValue.FALSE;
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof LegacyBooleanValue) {
			return ((LegacyBooleanValue) legacyValue).value;
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to boolean for field " + field);
		}
	}
}
