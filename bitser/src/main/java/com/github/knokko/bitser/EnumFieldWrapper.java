package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.legacy.LegacyEnumName;
import com.github.knokko.bitser.legacy.LegacyEnumOrdinal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import static com.github.knokko.bitser.IntegerBitser.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

@BitStruct(backwardCompatible = false)
class EnumFieldWrapper extends BitFieldWrapper {

	@BitField
	private final BitEnum.Mode mode;

	@IntegerField(expectUniform = false, minValue = 0)
	private final int numEnumConstants;

	@IntegerField(expectUniform = false, minValue = 1)
	private final int minNameLength;

	@IntegerField(expectUniform = false, minValue = 1)
	private final int maxNameLength;

	EnumFieldWrapper(VirtualField field, BitEnum.Mode mode) {
		super(field);
		this.mode = mode;
		if (!field.type.isEnum()) throw new InvalidBitFieldException("BitEnum can only be used on enums, but got " + field);
		Enum<?>[] enumConstants = (Enum<?>[]) field.type.getEnumConstants();
		this.numEnumConstants = enumConstants.length;
		int minNameLength = Integer.MAX_VALUE;
		int maxNameLength = 0;
		for (Enum<?> constant : enumConstants) {
			int length = constant.name().length();
			minNameLength = min(length, minNameLength);
			maxNameLength = max(length, maxNameLength);
		}
		this.minNameLength = minNameLength;
		this.maxNameLength = maxNameLength;
	}

	@SuppressWarnings("unused")
	private EnumFieldWrapper() {
		super();
		this.mode = BitEnum.Mode.Name;
		this.numEnumConstants = 0;
		this.minNameLength = 0;
		this.maxNameLength = 0;
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		Enum<?> enumValue = (Enum<?>) value;
		if (mode == BitEnum.Mode.Name) {
			IntegerField.Properties lengthField = new IntegerField.Properties(
					minNameLength, maxNameLength, true, 0, new long[0]
			);
			StringBitser.encode(enumValue.name(), lengthField, serializer.output);
		} else if (mode == BitEnum.Mode.Ordinal) {
			int maxOrdinal = field.type.getEnumConstants().length - 1;
			serializer.output.prepareProperty("enum-ordinal");
			encodeUniformInteger(enumValue.ordinal(), 0, maxOrdinal, serializer.output);
			serializer.output.finishProperty();
		} else throw new UnexpectedBitserException("Unknown enum mode: " + mode);
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		if (mode == BitEnum.Mode.Name) {
			IntegerField.Properties lengthField = new IntegerField.Properties(
					minNameLength, maxNameLength, true, 0, new long[0]
			);
			String name = StringBitser.decode(lengthField, deserializer.sizeLimit, deserializer.input);
			if (field.type == null) return name;
			try {
				return getConstantByName(name);
			} catch (NoSuchFieldException e) {
				throw new InvalidBitFieldException("Missing enum constant " + name + " in " + field.type);
			}
		}

		int ordinal;
		if (mode == BitEnum.Mode.Ordinal) {
			deserializer.input.prepareProperty("enum-ordinal");
			ordinal = (int) decodeUniformInteger(0, numEnumConstants - 1, deserializer.input);
			deserializer.input.finishProperty();
		} else throw new UnexpectedBitserException("Unknown BitEnum mode: " + mode);

		if (field.type == null) return ordinal;

		Object[] constants = field.type.getEnumConstants();
		if (ordinal >= constants.length) {
			throw new InvalidBitFieldException("Missing enum ordinal " + ordinal + " in " + field.type);
		}
		return constants[ordinal];
	}

	@Override
	Object read(BackReadParameters parameters) throws Throwable {
		if (mode == BitEnum.Mode.Name) {
			IntegerField.Properties lengthField = new IntegerField.Properties(
					minNameLength, maxNameLength, true, 0, new long[0]
			);
			String name = StringBitser.decode(
					lengthField, parameters.deserializer().sizeLimit, parameters.deserializer().input
			);
			return new LegacyEnumName(name);
		}

		if (mode == BitEnum.Mode.Ordinal) {
			parameters.deserializer().input.prepareProperty("enum-ordinal");
			int ordinal = (int) decodeUniformInteger(
					0, numEnumConstants - 1, parameters.deserializer().input
			);
			parameters.deserializer().input.finishProperty();
			return new LegacyEnumOrdinal(ordinal);
		} else throw new UnexpectedBitserException("Unknown BitEnum mode: " + mode);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object legacyValue, RecursionNode parentNode, String fieldName) {
		if (legacyValue instanceof LegacyEnumName) {
			try {
				return getConstantByName(((LegacyEnumName) legacyValue).name());
			} catch (NoSuchFieldException fieldNoLongerExists) {
				throw new LegacyBitserException("Missing legacy " + legacyValue + " in " + field);
			}
		} else if (legacyValue instanceof LegacyEnumOrdinal) {
			int ordinal = ((LegacyEnumOrdinal) legacyValue).ordinal();
			Object[] constants = field.type.getEnumConstants();
			if (ordinal >= constants.length) {
				throw new LegacyBitserException("Missing legacy ordinal " + ordinal + " in " + field);
			}
			return constants[ordinal];
		} else {
			throw new LegacyBitserException("Can't convert from legacy " + legacyValue + " to " + field.type + " for field " + field);
		}
	}

	private Object getConstantByName(String name) throws NoSuchFieldException {
		try {
			Field constantField = field.type.getDeclaredField(name);
			if (!Modifier.isPublic(field.type.getModifiers())) constantField.setAccessible(true);
			return constantField.get(null);
		} catch (IllegalAccessException e) {
			throw new UnexpectedBitserException("Failed to access enum constant " + name + " of " + field.type);
		}
	}

	@Override
	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		if (value != null) {
			Enum<?> enumValue = (Enum<?>) value;
			if (mode == BitEnum.Mode.Ordinal) computer.digest.update((byte) enumValue.ordinal());
			if (mode == BitEnum.Mode.Name) computer.digest.update(enumValue.name().getBytes(StandardCharsets.UTF_8));
		} else computer.digest.update((byte) 77);
	}
}
