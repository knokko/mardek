package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestByteCollectionField {

	@BitStruct(backwardCompatible = false)
	private static class BooleanArray {

		@NestedFieldSetting(path = "", writeAsBytes = true)
		boolean[] data;
	}

	@Test
	public void testBooleanArrayMultipleOf8() {
		BooleanArray array = new BooleanArray();
		array.data = new boolean[] {true, false, true, true, true, false, true, false};

		array = new Bitser().stupidDeepCopy(array);
		assertArrayEquals(new boolean[] {true, false, true, true, true, false, true, false}, array.data);
	}

	@Test
	public void testBooleanArrayNoMultipleOf8() {
		BooleanArray array = new BooleanArray();
		array.data = new boolean[] {true, false, true, true};

		array = new Bitser().stupidDeepCopy(array);
		assertArrayEquals(new boolean[] {true, false, true, true}, array.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class ByteArray {

		@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
		byte[] data;
	}

	@Test
	public void testByteArray() {
		ByteArray array = new ByteArray();
		Bitser bitser = new Bitser();
		ByteArray loaded = bitser.stupidDeepCopy(array);
		assertNull(loaded.data);

		array.data = new byte[0];
		loaded = bitser.stupidDeepCopy(array);
		assertEquals(0, loaded.data.length);

		array.data = new byte[]{-128, -1, 0, 1, 127};
		loaded = bitser.stupidDeepCopy(array);
		assertArrayEquals(new byte[]{-128, -1, 0, 1, 127}, loaded.data);
	}

	@Test
	public void testByteArrayDeepEqualsAndHashCode() {
		ByteArray a = new ByteArray();
		ByteArray b = new ByteArray();

		Bitser bitser = new Bitser();
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.data = new byte[] { 12 };
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.data = new byte[] { 12 };
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.data[0] = 13;
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));
	}

	@BitStruct(backwardCompatible = false)
	private static class IntArray {

		@NestedFieldSetting(path = "", writeAsBytes = true)
		final int[] data;

		@SuppressWarnings("unused")
		IntArray() {
			this.data = null;
		}

		IntArray(int[] data) {
			this.data = data;
		}
	}

	@Test
	public void testIntArray() {
		Bitser bitser = new Bitser();
		IntArray nullArray = new IntArray(null);
		String errorMessage = assertThrows(
				InvalidBitValueException.class, () -> bitser.toBytes(nullArray)
		).getMessage();
		assertContains(errorMessage, "must not be null");

		IntArray empty = new IntArray(new int[0]);
		IntArray loaded = bitser.stupidDeepCopy(empty);
		assert loaded.data != null;
		assertEquals(0, loaded.data.length);

		IntArray filled = new IntArray(new int[]{Integer.MIN_VALUE, -1234, -1, 0, 10, 12345, Integer.MAX_VALUE});
		loaded = bitser.stupidDeepCopy(filled);
		assert loaded.data != null;
		assertArrayEquals(new int[]{Integer.MIN_VALUE, -1234, -1, 0, 10, 12345, Integer.MAX_VALUE}, loaded.data);
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidOptional {

		@SuppressWarnings("unused")
		@NestedFieldSetting(path = "", writeAsBytes = true)
		@NestedFieldSetting(path = "c", optional = true)
		byte[] data = new byte[10];
	}

	@Test
	public void testInvalidOptional() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().stupidDeepCopy(new InvalidOptional())
		).getMessage();
		assertContains(errorMessage, "NestedFieldSetting's on writeAsBytes targets is forbidden:");
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidAnnotations {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		byte[] data = new byte[10];
	}

	@Test
	public void testInvalidAnnotations() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().stupidDeepCopy(new InvalidAnnotations())
		).getMessage();
		assertContains(errorMessage, "Value annotations are forbidden when writeAsBytes is true");
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidType {

		@SuppressWarnings("unused")
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final String[] data = new String[0];
	}

	@Test
	public void testInvalidType() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().stupidDeepCopy(new InvalidType())
		).getMessage();
		assertContains(errorMessage, "Unexpected write-as-bytes field type class");
	}
}
