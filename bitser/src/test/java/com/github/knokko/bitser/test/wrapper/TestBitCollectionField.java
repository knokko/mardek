package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.NestedFieldSetting;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.options.CollectionSizeLimit;
import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.exceptions.RecursionException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestBitCollectionField {

	@BitStruct(backwardCompatible = true)
	private static class Strings {

		@BitField(id = 1)
		@NestedFieldSetting(path = "c", optional = true)
		private String[] array;

		@BitField(id = 0)
		@NestedFieldSetting(path = "", optional = true)
		private ArrayList<String> list;
	}

	@Test
	public void testNullStrings() {
		Strings strings = new Strings();
		strings.array = new String[]{"hello", null, "world"};

		strings = new Bitser().stupidDeepCopy(strings);
		assertArrayEquals(new String[]{"hello", null, "world"}, strings.array);
		assertNull(strings.list);
	}

	@Test
	public void testNonNullStrings() {
		Strings strings = new Strings();
		strings.array = new String[]{"test1234"};
		strings.list = new ArrayList<>();
		strings.list.add("hello, world!");

		strings = new Bitser().stupidDeepCopy(strings);
		assertArrayEquals(new String[]{"test1234"}, strings.array);
		assertEquals(1, strings.list.size());
		assertEquals("hello, world!", strings.list.get(0));
	}

	@Test
	public void testStringArrayDeepEqualsAndHashCode() {
		Strings a = new Strings();
		Strings b = new Strings();

		Bitser bitser = new Bitser();
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.array = new String[] { "hello" };
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.array = new String[] { "hello" };
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.array = new String[] { "hello", "a" };
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.array = new String[] { "hello", "b" };
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.array[1] = null;
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.array[1] = null;
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.array[1] = "null";
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));
	}

	@Test
	public void testStringListDeepEqualsAndHashCode() {
		Strings a = new Strings();
		Strings b = new Strings();

		Bitser bitser = new Bitser();
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.list = new ArrayList<>();
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));
		a.list.add("hello");
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.list = new ArrayList<>();
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));
		b.list.add("hello");
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.list.add("a");
		b.list.add("b");
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));
	}

	@BitStruct(backwardCompatible = false)
	private static class Bytes {

		@IntegerField(expectUniform = true)
		@NestedFieldSetting(path = "", sizeField = @IntegerField(minValue = 2, maxValue = 2, expectUniform = true))
		private final byte[] array = new byte[2];

		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "c", optional = true)
		private final LinkedList<Byte> list = new LinkedList<>();
	}

	@Test
	public void testNullAndZeroBytes() {
		Bytes bytes = new Bytes();
		bytes.list.add((byte) 123);
		bytes.list.add(null);
		bytes.list.add((byte) 45);

		bytes = new Bitser().stupidDeepCopy(bytes);
		assertArrayEquals(new byte[2], bytes.array);
		assertEquals(3, bytes.list.size());
		assertEquals((byte) 123, bytes.list.get(0));
		assertNull(bytes.list.get(1));
		assertEquals((byte) 45, bytes.list.get(2));
	}

	@Test
	public void testNonNullBytes() {
		Bytes bytes = new Bytes();
		bytes.array[0] = -12;
		bytes.array[1] = 34;
		bytes.list.add((byte) -123);

		bytes = new Bitser().stupidDeepCopy(bytes);
		assertArrayEquals(new byte[]{-12, 34}, bytes.array);
		assertEquals(1, bytes.list.size());
		assertEquals((byte) -123, bytes.list.get(0));
	}

	@BitStruct(backwardCompatible = false)
	private static class InvalidShortArray {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "c", optional = true)
		private short[] values;
	}

	@Test
	public void testInvalidShortArray() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new InvalidShortArray())
		).getMessage();
		assertContains(errorMessage, "can't be optional");
	}

	@BitStruct(backwardCompatible = false)
	private static class Longs {

		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		public Long[] array;

		@IntegerField(expectUniform = false)
		public HashSet<Long> set = new HashSet<>();
	}

	@Test
	public void testNullLongArray() {
		Longs longs = new Longs();
		longs = new Bitser().stupidDeepCopy(longs);
		assertNull(longs.array);
		assertEquals(0, longs.set.size());
	}

	@Test
	public void testNullLongValues() {
		Longs longs = new Longs();
		longs.array = new Long[]{12L, null, 34L};
		longs = new Bitser().stupidDeepCopy(longs);
		assertArrayEquals(new Long[]{12L, null, 34L}, longs.array);
		assertEquals(0, longs.set.size());
	}

	@Test
	public void testForbidLongSetNull() {
		Longs longs = new Longs();
		longs.set = null;
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(longs, new BitOutputStream(new ByteArrayOutputStream()))
		).getMessage();
		assertContains(errorMessage, "must not be null");
	}

	@Test
	public void testForbidLongSetNullValues() {
		Longs longs = new Longs();
		longs.set.add(12L);
		longs.set.add(null);
		longs.set.add(34L);
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(longs, new BitOutputStream(new ByteArrayOutputStream()))
		).getMessage();
		assertContains(errorMessage, "must not have null elements");
	}

	@Test
	public void testProperLongValues() {
		Longs longs = new Longs();
		longs.array = new Long[]{-1L, 2L};
		longs.set.add(-1234L);

		longs = new Bitser().stupidDeepCopy(longs);
		assertArrayEquals(new Long[]{-1L, 2L}, longs.array);
		assertEquals(1, longs.set.size());
		assertEquals(-1234L, longs.set.iterator().next());
	}

	@Test
	public void testLongSetEquality() {
		Longs original = new Longs();
		for (long counter = 0; counter < 10_000; counter++) original.set.add(counter);

		var bitser = new Bitser();
		for (int counter = 0; counter < 10; counter++) {
			assertTrue(bitser.deepEquals(original, bitser.deepCopy(original)));
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingGenerics {

		@BitField
		@SuppressWarnings({"rawtypes", "unused"})
		ArrayList list = new ArrayList();
	}

	@Test
	public void testMissingGenerics() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().serialize(new MissingGenerics(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Unexpected generic type");
	}

	@BitStruct(backwardCompatible = false)
	private static class UnknownGenerics {

		@BitField
		@SuppressWarnings({"rawtypes", "unused"})
		ArrayList<?> list = new ArrayList();
	}

	@Test
	public void testUnknownGenerics() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().serialize(new UnknownGenerics(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Unexpected generic type");
	}

	@BitStruct(backwardCompatible = false)
	private static class WithAbstractList {

		@BitField
		@SuppressWarnings("unused")
		List<String> list = new ArrayList<>();
	}

	@Test
	public void testAbstractList() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().serialize(new WithAbstractList(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Field type must not be abstract or an interface");
		assertContains(errorMessage, "WithAbstractList.list");
	}

	@BitStruct(backwardCompatible = false)
	private static class ReferenceList {

		@ReferenceField(stable = false, label = "refs")
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		final ArrayList<String> list = new ArrayList<>();
	}

	@Test
	public void testReferenceListDeepEqualsAndHashCode() {
		ReferenceList a = new ReferenceList();
		ReferenceList b = new ReferenceList();

		Bitser bitser = new Bitser();
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));
		assertTrue(bitser.deepEquals(null, null));
		assertEquals(0, bitser.hashCode(null));
		assertFalse(bitser.deepEquals(null, b));
		assertNotEquals(bitser.hashCode(null), bitser.hashCode(b));

		String hello1 = "hello";
		@SuppressWarnings("StringOperationCanBeSimplified")
		String hello2 = new String(hello1);

		a.list.add(hello1);
		assertFalse(bitser.deepEquals(a, b));
		b.list.add(hello1);
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));
		a.list.add(hello1);
		b.list.add(hello2);
		assertFalse(bitser.deepEquals(a, b));
	}

	private static class CollectionWithWeirdConstructors<T> extends ArrayList<T> {

		CollectionWithWeirdConstructors(int left, int right) {
			super(left + right);
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class StructWithWeirdCollection {

		@BitField(id = 0)
		@SuppressWarnings("unused")
		final CollectionWithWeirdConstructors<String> collection = new CollectionWithWeirdConstructors<>(1, 2);
	}

	@Test
	public void testCollectionWithWeirdConstructors() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().stupidDeepCopy(new StructWithWeirdCollection())
		).getMessage();
		assertContains(errorMessage, "-> collection");
		assertContains(errorMessage, "Failed to find constructor of class");
		assertContains(errorMessage, "TestBitCollectionField$CollectionWithWeirdConstructors");
	}

	private static class AggressiveCollection<T> extends ArrayList<T> {

		public AggressiveCollection() {
			super();
		}

		@SuppressWarnings("unused")
		public AggressiveCollection(int capacity) {
			super(capacity);
			throw new UnsupportedOperationException("Nah");
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class StructWithAggressiveCollection {

		@BitField
		@SuppressWarnings("unused")
		final AggressiveCollection<UUID> collection = new AggressiveCollection<>();
	}

	@Test
	public void testCollectionWithAggressiveConstructor() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().stupidDeepCopy(new StructWithAggressiveCollection())
		).getMessage();
		assertContains(errorMessage, "-> collection");
		assertContains(errorMessage, "Failed to instantiate class");
		assertContains(errorMessage, "TestBitCollectionField$AggressiveCollection");
	}

	private static class NotGenericCollection extends LinkedList<String> {}

	@BitStruct(backwardCompatible = false)
	private static class StructWithNotGenericCollection {

		@BitField
		@SuppressWarnings("unused")
		final NotGenericCollection collection = new NotGenericCollection();
	}

	@Test
	public void testNotGenericCollection() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().stupidDeepCopy(new StructWithNotGenericCollection())
		).getMessage();
		assertContains(errorMessage, "Unexpected generic type");
		assertContains(errorMessage, "NotGenericCollection");
		assertContains(errorMessage, "TestBitCollectionField$StructWithNotGenericCollection.collection");
	}

	@BitStruct(backwardCompatible = false)
	private static class StringList {

		@BitField
		@SuppressWarnings("unused")
		final ArrayList<String> strings = new ArrayList<>();
	}

	@Test
	public void testLargeMemoryAllocationAttack() throws IOException {
		Bitser bitser = new Bitser();

		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);
		IntegerBitser.encodeUnknownLength(Integer.MAX_VALUE, bitOutput);
		bitOutput.finish();

		// Without limit
		RecursionException exception = assertThrows(
				RecursionException.class,
				() -> bitser.fromBytes(Strings.class, byteOutput.toByteArray())
		);
		assertInstanceOf(OutOfMemoryError.class, exception.getCause());

		// With limit: String[]
		String errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.fromBytes(
				Strings.class, byteOutput.toByteArray(), new CollectionSizeLimit(1000)
		)).getMessage();
		assertContains(errorMessage, "-> array");
		assertContains(errorMessage, "2147483647 exceeds the size limit of 1000");

		// With limit: ArrayList<String>
		errorMessage = assertThrows(InvalidBitValueException.class, () -> bitser.fromBytes(
				StringList.class, byteOutput.toByteArray(), new CollectionSizeLimit(1000)
		)).getMessage();
		assertContains(errorMessage, "-> strings");
		assertContains(errorMessage, "2147483647 exceeds the size limit of 1000");
	}
}
