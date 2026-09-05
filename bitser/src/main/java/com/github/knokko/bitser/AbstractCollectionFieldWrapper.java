package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;

import static java.lang.Math.max;
import static java.lang.Math.min;

abstract class AbstractCollectionFieldWrapper extends BitFieldWrapper {

	static Object constructCollectionWithSize(Class<?> fieldType, Class<?> keysType, int size) {
		try {
			try {
				return fieldType.getConstructor(int.class).newInstance(size);
			} catch (NoSuchMethodException noIntConstructor) {
				try {
					return fieldType.getConstructor().newInstance();
				} catch (NoSuchMethodException noEmptyConstructor) {
					try {
						return fieldType.getConstructor(Class.class).newInstance(keysType);
					} catch (NoSuchMethodException unexpected) {
						throw new InvalidBitFieldException(
								"Failed to find constructor of " + fieldType + ": bitser requires either a constructor " +
										"with no arguments, or a constructor with exactly 1 argument whose type is int");
					}
				}
			}
		} catch (Exception constructorFailed) {
			throw new InvalidBitFieldException("Failed to instantiate " + fieldType + ": " + constructorFailed.getMessage());
		}
	}

	@BitField
	final IntegerField.Properties sizeField;

	@BitField(optional = true)
	private final ArrayType arrayType;

	AbstractCollectionFieldWrapper(VirtualField field, IntegerField sizeField) {
		super(field);
		if (sizeField.minValue() > Integer.MAX_VALUE || sizeField.maxValue() < 0) {
			throw new InvalidBitFieldException("Invalid size field");
		}
		if (!field.type.isArray() && (field.type.isInterface() || Modifier.isAbstract(field.type.getModifiers()))) {
			throw new InvalidBitFieldException("Field type must not be abstract or an interface: " + field);
		}
		this.sizeField = new IntegerField.Properties(
				max(0, sizeField.minValue()), min(Integer.MAX_VALUE, sizeField.maxValue()),
				sizeField.expectUniform(), sizeField.digitSize(), sizeField.commonValues()
		);
		this.arrayType = determineArrayType();
	}

	AbstractCollectionFieldWrapper() {
		super();
		this.sizeField = new IntegerField.Properties();
		this.arrayType = null;
	}

	abstract ArrayType determineArrayType();

	protected Object constructCollectionWithSize(int size) {
		if (field.type == null) {
			if (arrayType == null) return new Object[size];
			return switch (arrayType) {
				case BOOLEAN -> new boolean[size];
				case BYTE -> new byte[size];
				case SHORT -> new short[size];
				case CHAR -> new char[size];
				case INT -> new int[size];
				case FLOAT -> new float[size];
				case LONG -> new long[size];
				case DOUBLE -> new double[size];
			};
		}
		if (field.type.isArray()) {
			return Array.newInstance(field.type.getComponentType(), size);
		} else {
			return constructCollectionWithSize(field.type, null, size);
		}
	}

	@BitEnum(mode = BitEnum.Mode.Ordinal)
	enum ArrayType {
		BOOLEAN,
		BYTE,
		SHORT,
		CHAR,
		INT,
		FLOAT,
		LONG,
		DOUBLE
	}
}
