package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.StringField;
import com.github.knokko.bitser.legacy.LegacyStringValue;

import java.nio.charset.StandardCharsets;

import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class StringFieldWrapper extends BitFieldWrapper {

	@BitField
	private final IntegerField.Properties lengthField;

	StringFieldWrapper(VirtualField field, StringField stringField) {
		super(field);
		long minLength = 0;
		long maxLength = Integer.MAX_VALUE;
		boolean expectUniform = false;
		int digitSize = IntegerField.DIGIT_SIZE_TERMINATORS;
		long[] commonValues = new long[0];
		if (stringField != null) {
			minLength = max(minLength, stringField.length().minValue());
			maxLength = min(maxLength, stringField.length().maxValue());
			expectUniform = stringField.length().expectUniform();
			digitSize = stringField.length().digitSize();
			commonValues = stringField.length().commonValues();
		}
		this.lengthField = new IntegerField.Properties(minLength, maxLength, expectUniform, digitSize, commonValues);
	}

	@SuppressWarnings("unused")
	private StringFieldWrapper() {
		super();
		this.lengthField = new IntegerField.Properties();
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		if (serializer.intDistribution != null) {
			long length = ((String) value).getBytes(StandardCharsets.UTF_8).length;
			serializer.intDistribution.insert(field + " string length", length, lengthField);
			serializer.intDistribution.insert("string length", length, lengthField);
		}
		StringBitser.encode((String) value, lengthField, serializer.output);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		return StringBitser.decode(lengthField, deserializer.sizeLimit, deserializer.input);
	}

	@Override
	Object read(BackReadParameters parameters) throws Throwable {
		return new LegacyStringValue(StringBitser.decode(
				lengthField, parameters.deserializer().sizeLimit, parameters.deserializer().input
		));
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof LegacyStringValue) {
			return ((LegacyStringValue) legacyValue).value();
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to String for field " + field);
		}
	}
}
