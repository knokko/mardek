package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.legacy.*;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Objects;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;

@BitStruct(backwardCompatible = false)
class ByteCollectionFieldWrapper extends AbstractCollectionFieldWrapper {

	ByteCollectionFieldWrapper(VirtualField field, IntegerField sizeField) {
		super(field, sizeField);
	}

	@SuppressWarnings("unused")
	private ByteCollectionFieldWrapper() {
		super();
	}

	@Override
	AbstractCollectionFieldWrapper.ArrayType determineArrayType() {
		if (field.type == boolean[].class) return ArrayType.BOOLEAN;
		if (field.type == byte[].class) return ArrayType.BYTE;
		if (field.type == short[].class) return ArrayType.SHORT;
		if (field.type == char[].class) return ArrayType.CHAR;
		if (field.type == int[].class) return ArrayType.INT;
		if (field.type == float[].class) return ArrayType.FLOAT;
		if (field.type == long[].class) return ArrayType.LONG;
		if (field.type == double[].class) return ArrayType.DOUBLE;
		throw new InvalidBitFieldException("Unexpected write-as-bytes field type " + field.type);
	}

	private byte[] toByteArray(boolean[] booleans) {
		int numBytes = booleans.length / 8;
		if (booleans.length % 8 != 0) numBytes += 1;

		byte[] bytes = new byte[numBytes];
		for (int boolIndex = 0; boolIndex < booleans.length; boolIndex++) {
			if (booleans[boolIndex]) bytes[boolIndex >> 3] |= (byte) (1 << (boolIndex & 7));
		}
		return bytes;
	}

	private byte[] toByteArray(short[] shorts) {
		byte[] bytes = new byte[2 * shorts.length];
		for (int index = 0; index < shorts.length; index++) {
			short element = shorts[index];
			int byteIndex = 2 * index;
			bytes[byteIndex] = (byte) (element >> 8);
			bytes[byteIndex + 1] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(char[] chars) {
		byte[] bytes = new byte[2 * chars.length];
		for (int index = 0; index < chars.length; index++) {
			char element = chars[index];
			int byteIndex = 2 * index;
			bytes[byteIndex] = (byte) (element >> 8);
			bytes[byteIndex + 1] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(int[] ints) {
		byte[] bytes = new byte[4 * ints.length];
		for (int index = 0; index < ints.length; index++) {
			int element = ints[index];
			int byteIndex = 4 * index;
			bytes[byteIndex] = (byte) (element >> 24);
			bytes[byteIndex + 1] = (byte) (element >> 16);
			bytes[byteIndex + 2] = (byte) (element >> 8);
			bytes[byteIndex + 3] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(float[] floats) {
		byte[] bytes = new byte[4 * floats.length];
		for (int index = 0; index < floats.length; index++) {
			int element = floatToRawIntBits(floats[index]);
			int byteIndex = 4 * index;
			bytes[byteIndex] = (byte) (element >> 24);
			bytes[byteIndex + 1] = (byte) (element >> 16);
			bytes[byteIndex + 2] = (byte) (element >> 8);
			bytes[byteIndex + 3] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(long[] longs) {
		byte[] bytes = new byte[8 * longs.length];
		for (int index = 0; index < longs.length; index++) {
			long element = longs[index];
			int byteIndex = 8 * index;
			bytes[byteIndex] = (byte) (element >> 56);
			bytes[byteIndex + 1] = (byte) (element >> 48);
			bytes[byteIndex + 2] = (byte) (element >> 40);
			bytes[byteIndex + 3] = (byte) (element >> 32);
			bytes[byteIndex + 4] = (byte) (element >> 24);
			bytes[byteIndex + 5] = (byte) (element >> 16);
			bytes[byteIndex + 6] = (byte) (element >> 8);
			bytes[byteIndex + 7] = (byte) element;
		}
		return bytes;
	}

	private byte[] toByteArray(double[] doubles) {
		byte[] bytes = new byte[8 * doubles.length];
		for (int index = 0; index < doubles.length; index++) {
			long element = doubleToRawLongBits(doubles[index]);
			int byteIndex = 8 * index;
			bytes[byteIndex] = (byte) (element >> 56);
			bytes[byteIndex + 1] = (byte) (element >> 48);
			bytes[byteIndex + 2] = (byte) (element >> 40);
			bytes[byteIndex + 3] = (byte) (element >> 32);
			bytes[byteIndex + 4] = (byte) (element >> 24);
			bytes[byteIndex + 5] = (byte) (element >> 16);
			bytes[byteIndex + 6] = (byte) (element >> 8);
			bytes[byteIndex + 7] = (byte) element;
		}
		return bytes;
	}

	private byte[] getBytesToWrite(Object value) {
		if (value instanceof boolean[]) return toByteArray((boolean[]) value);
		else if (value instanceof byte[]) return (byte[]) value;
		else if (value instanceof short[]) return toByteArray((short[]) value);
		else if (value instanceof char[]) return toByteArray((char[]) value);
		else if (value instanceof int[]) return toByteArray((int[]) value);
		else if (value instanceof float[]) return toByteArray((float[]) value);
		else if (value instanceof long[]) return toByteArray((long[]) value);
		else if (value instanceof double[]) return toByteArray((double[]) value);
		else throw new UnexpectedBitserException("Can't encode " + value.getClass() + " as bytes");
	}

	@Override
	public void write(
			Serializer serializer, Object value,
			RecursionNode parentNode, String fieldName
	) throws Throwable {
		int length = Array.getLength(value);
		if (serializer.intDistribution != null) {
			serializer.intDistribution.insert(field + " collection size", (long) length, sizeField);
			serializer.intDistribution.insert("collection size", (long) length, sizeField);
		}

		serializer.output.prepareProperty("byte-collection-length");
		IntegerBitser.encodeInteger(length, sizeField, serializer.output);
		serializer.output.finishProperty();

		serializer.output.prepareProperty("byte-collection-bytes");
		serializer.output.write(getBytesToWrite(value));
		serializer.output.finishProperty();
	}

	private void readData(BitInputStream input, Object array) throws IOException {
		input.prepareProperty("byte-collection-bytes");
		if (array instanceof boolean[]) backToBooleanArray((boolean[]) array, input);
		else if (array instanceof byte[]) input.read((byte[]) array);
		else if (array instanceof short[]) backToShortArray((short[]) array, input);
		else if (array instanceof char[]) backToCharArray((char[]) array, input);
		else if (array instanceof int[]) backToIntArray((int[]) array, input);
		else if (array instanceof float[]) backToFloatArray((float[]) array, input);
		else if (array instanceof long[]) backToLongArray((long[]) array, input);
		else if (array instanceof double[]) backToDoubleArray((double[]) array, input);
		else throw new InvalidBitFieldException("Can't decode " + array.getClass() + " from bytes");
		input.finishProperty();
	}

	@Override
	public Object read(Deserializer deserializer, RecursionNode parentNode, String fieldName) throws Throwable {
		deserializer.input.prepareProperty("byte-collection-length");
		int size = IntegerBitser.decodeLength(sizeField, deserializer.sizeLimit, "size", deserializer.input);
		deserializer.input.finishProperty();
		Object value = Array.newInstance(field.type.getComponentType(), size);
		readData(deserializer.input, value);
		return value;
	}

	@Override
	Object read(BackReadParameters parameters) throws Throwable {
		parameters.deserializer().input.prepareProperty("byte-collection-length");
		int size = IntegerBitser.decodeLength(
				sizeField, parameters.deserializer().sizeLimit,
				"size", parameters.deserializer().input
		);
		parameters.deserializer().input.finishProperty();
		Object value = constructCollectionWithSize(size);
		readData(parameters.deserializer().input, value);
		return new LegacyArrayValue(value);
	}

	@Override
	Object convert(BackDeserializer deserializer, Object rawLegacyValue, RecursionNode parentNode, String fieldName) {
		if (!(rawLegacyValue instanceof LegacyArrayValue legacyArrayWrapper)) {
			throw new LegacyBitserException(
					"Can't convert from legacy " + rawLegacyValue + " to " + field.type + " for field " + field
			);
		}

		Object legacyArray = legacyArrayWrapper.array;
		if (field.type == legacyArray.getClass()) {
			return legacyArray;
		}

		int size = Array.getLength(legacyArray);
		Object newArray = Array.newInstance(field.type.getComponentType(), size);
		legacyArrayWrapper.modernObject = newArray;
		for (int index = 0; index < size; index++) {
			Object legacyValue = Array.get(legacyArray, index);
			setFromLegacyValue(newArray, index, legacyValue);
		}

		return newArray;
	}

	void backToBooleanArray(boolean[] booleans, BitInputStream input) throws IOException {
		int numBytes = booleans.length / 8;
		if (booleans.length % 8 != 0) numBytes += 1;
		byte[] bytes = new byte[numBytes];
		input.read(bytes);

		for (int boolIndex = 0; boolIndex < booleans.length; boolIndex++) {
			booleans[boolIndex] = (bytes[boolIndex >> 3] & (1 << (boolIndex & 7))) != 0;
		}
	}

	void backToShortArray(short[] shorts, BitInputStream input) throws IOException {
		byte[] bytes = new byte[2 * shorts.length];
		input.read(bytes);

		for (int shortIndex = 0; shortIndex < shorts.length; shortIndex++) {
			int byteIndex = 2 * shortIndex;
			int byte8 = toUnsignedInt(bytes[byteIndex]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 1]);
			shorts[shortIndex] = (short) ((byte8 << 8) | byte0);
		}
	}

	void backToCharArray(char[] chars, BitInputStream input) throws IOException {
		byte[] bytes = new byte[2 * chars.length];
		input.read(bytes);

		for (int charIndex = 0; charIndex < chars.length; charIndex++) {
			int byteIndex = 2 * charIndex;
			int byte8 = toUnsignedInt(bytes[byteIndex]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 1]);
			chars[charIndex] = (char) ((byte8 << 8) | byte0);
		}
	}

	void backToIntArray(int[] ints, BitInputStream input) throws IOException {
		byte[] bytes = new byte[4 * ints.length];
		input.read(bytes);

		for (int intIndex = 0; intIndex < ints.length; intIndex++) {
			int byteIndex = 4 * intIndex;
			int byte24 = toUnsignedInt(bytes[byteIndex]);
			int byte16 = toUnsignedInt(bytes[byteIndex + 1]);
			int byte8 = toUnsignedInt(bytes[byteIndex + 2]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 3]);
			ints[intIndex] = (byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0;
		}
	}

	void backToFloatArray(float[] floats, BitInputStream input) throws IOException {
		byte[] bytes = new byte[4 * floats.length];
		input.read(bytes);

		for (int floatIndex = 0; floatIndex < floats.length; floatIndex++) {
			int byteIndex = 4 * floatIndex;
			int byte24 = toUnsignedInt(bytes[byteIndex]);
			int byte16 = toUnsignedInt(bytes[byteIndex + 1]);
			int byte8 = toUnsignedInt(bytes[byteIndex + 2]);
			int byte0 = toUnsignedInt(bytes[byteIndex + 3]);
			floats[floatIndex] = intBitsToFloat((byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0);
		}
	}

	void backToLongArray(long[] longs, BitInputStream input) throws IOException {
		byte[] bytes = new byte[8 * longs.length];
		input.read(bytes);

		for (int longIndex = 0; longIndex < longs.length; longIndex++) {
			int byteIndex = 8 * longIndex;
			long byte56 = toUnsignedInt(bytes[byteIndex]);
			long byte48 = toUnsignedInt(bytes[byteIndex + 1]);
			long byte40 = toUnsignedInt(bytes[byteIndex + 2]);
			long byte32 = toUnsignedInt(bytes[byteIndex + 3]);
			long byte24 = toUnsignedInt(bytes[byteIndex + 4]);
			long byte16 = toUnsignedInt(bytes[byteIndex + 5]);
			long byte8 = toUnsignedInt(bytes[byteIndex + 6]);
			long byte0 = toUnsignedInt(bytes[byteIndex + 7]);
			longs[longIndex] = (byte56 << 56) | (byte48 << 48) | (byte40 << 40) | (byte32 << 32) |
					(byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0;
		}
	}

	void backToDoubleArray(double[] doubles, BitInputStream input) throws IOException {
		byte[] bytes = new byte[8 * doubles.length];
		input.read(bytes);

		for (int doubleIndex = 0; doubleIndex < doubles.length; doubleIndex++) {
			int byteIndex = 8 * doubleIndex;
			long byte56 = toUnsignedInt(bytes[byteIndex]);
			long byte48 = toUnsignedInt(bytes[byteIndex + 1]);
			long byte40 = toUnsignedInt(bytes[byteIndex + 2]);
			long byte32 = toUnsignedInt(bytes[byteIndex + 3]);
			long byte24 = toUnsignedInt(bytes[byteIndex + 4]);
			long byte16 = toUnsignedInt(bytes[byteIndex + 5]);
			long byte8 = toUnsignedInt(bytes[byteIndex + 6]);
			long byte0 = toUnsignedInt(bytes[byteIndex + 7]);
			doubles[doubleIndex] = longBitsToDouble((byte56 << 56) | (byte48 << 48) | (byte40 << 40) | (byte32 << 32) |
					(byte24 << 24) | (byte16 << 16) | (byte8 << 8) | byte0);
		}
	}

	private void setFromLegacyValue(Object newArray, int index, Object legacyValue) {
		if (legacyValue == null) {
			Array.set(newArray, index, null);
			return;
		}
		Object newNumber = convertLegacyNumber(legacyValue);
		Array.set(newArray, index, newNumber);
	}

	private Object convertLegacyNumber(Object legacyValue) {
		if (legacyValue instanceof Boolean) return legacyValue;
		if (legacyValue instanceof LegacyBooleanValue) return ((LegacyBooleanValue) legacyValue).value;
		Number legacyNumber;
		if (legacyValue instanceof Character) legacyNumber = (int) ((char) legacyValue);
		else if (legacyValue instanceof LegacyFloatValue) legacyNumber = ((LegacyFloatValue) legacyValue).value();
		else if (legacyValue instanceof LegacyIntValue) legacyNumber = ((LegacyIntValue) legacyValue).value();
		else legacyNumber = (Number) legacyValue;
		if (field.type == byte[].class) return legacyNumber.byteValue();
		if (field.type == short[].class) return legacyNumber.shortValue();
		if (field.type == char[].class) return (char) legacyNumber.intValue();
		if (field.type == int[].class) return legacyNumber.intValue();
		if (field.type == float[].class) return legacyNumber.floatValue();
		if (field.type == long[].class) return legacyNumber.longValue();
		if (field.type == double[].class) return legacyNumber.doubleValue();
		throw new UnexpectedBitserException("Unexpected write-as-bytes type " + field.type);
	}

	@Override
	Object deepCopy(
			Object original, DeepCopyMachine machine,
			RecursionNode parentNode, String fieldName
	) {
		if (original == null) return null;

		if (original instanceof Collection<?> originalCollection) {
			@SuppressWarnings("unchecked")
			var newCollection = (Collection<Object>) constructCollectionWithSize(originalCollection.size());
			newCollection.addAll(originalCollection);
			return newCollection;
		} else {
			int size = Array.getLength(original);
			var newArray = constructCollectionWithSize(size);
			//noinspection SuspiciousSystemArraycopy
			System.arraycopy(original, 0, newArray, 0, size);
			return newArray;
		}
	}

	@Override
	void collectInstances(InstanceCollector collector, Object value, RecursionNode parentNode, String fieldName) {
		if (value instanceof Collection<?> collection) {
			for (Object element : collection) {
				if (element != null) collector.register(element);
			}
		} else {
			int length = Array.getLength(value);
			for (int index = 0; index < length; index++) {
				Object element = Array.get(value, index);
				if (element != null) collector.register(element);
			}
		}
	}

	@Override
	void hashCode(HashComputer computer, Object value, RecursionNode parentNode, String fieldName) {
		if (value == null) computer.digest.update((byte) 100);
		else computer.digest.update(getBytesToWrite(value));
	}

	@Override
	boolean certainlyNotEqual(
			DeepComparator comparator, Object valueA, Object valueB,
			RecursionNode node, String fieldName
	) {
		return !Objects.deepEquals(valueA, valueB);
	}
}
