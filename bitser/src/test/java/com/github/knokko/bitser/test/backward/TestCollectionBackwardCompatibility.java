package com.github.knokko.bitser.test.backward;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestCollectionBackwardCompatibility {

	@BitStruct(backwardCompatible = true)
	private static class ShallowBefore {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		private static final boolean KEY_PROPERTIES = false;

		@SuppressWarnings("unused")
		@FloatField
		private static final boolean VALUE_PROPERTIES = false;

		@BitField(id = 2)
		@NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES")
		@NestedFieldSetting(path = "v", fieldName = "VALUE_PROPERTIES", optional = true)
		final HashMap<Integer, Float> sinTable = new HashMap<>();

		@BitField(id = 5)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		short[] heights;

		@BitField(id = 4)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		final ArrayList<String> lines = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class ShallowAfter {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true)
		private static final boolean KEY_PROPERTIES = false;

		@SuppressWarnings("unused")
		@FloatField(expectMultipleOf = 10.0)
		private static final boolean VALUE_PROPERTIES = false;

		@BitField(id = 2)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES")
		@NestedFieldSetting(path = "v", fieldName = "VALUE_PROPERTIES", optional = true)
		final HashMap<Integer, Float> sinTable = new HashMap<>();

		@BitField(id = 5)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		int[] heights;

		@BitField(id = 4)
		@NestedFieldSetting(path = "")
		@NestedFieldSetting(path = "c", optional = true)
		final ArrayList<String> lines = new ArrayList<>();
	}

	@Test
	public void testShallowCollectionsBackwardCompatibility() {
		Bitser bitser = new Bitser();
		ShallowBefore before = new ShallowBefore();
		before.sinTable.put(0, 0f);
		before.sinTable.put(90, 1f);
		before.sinTable.put(180, 0f);
		before.sinTable.put(270, -1f);
		before.sinTable.put(1234, null);
		before.heights = new short[] {123, -76, 43, 12345};
		before.lines.add("shallow");
		before.lines.add(null);
		before.lines.add("compatibility");

		ShallowAfter after = bitser.fromBytes(
				ShallowAfter.class,
				bitser.toBytes(before, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(before.sinTable, after.sinTable);
		assertArrayEquals(new int[] {123, -76, 43, 12345}, after.heights);
		assertEquals(before.lines, after.lines);

		ShallowBefore back = bitser.fromBytes(
				ShallowBefore.class,
				bitser.toBytes(after, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(before.sinTable, back.sinTable);
		assertArrayEquals(before.heights, back.heights);
		assertEquals(before.lines, back.lines);
	}

	@SuppressWarnings("SpellCheckingInspection")
	@BitStruct(backwardCompatible = true)
	private static class WriteBytesBefore {

		@BitField(id = 4)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final boolean[] bools1 = new boolean[5];

		@BitField(id = 7)
		@NestedFieldSetting(
				path = "", writeAsBytes = true, optional = true,
				sizeField = @IntegerField(expectUniform = true, minValue = 2, maxValue = 2)
		)
		final boolean[] bools2 = new boolean[2];

		@BitField(id = 1)
		@NestedFieldSetting(path = "", writeAsBytes = true, optional = true)
		final byte[] bytes1 = new byte[5];

		@BitField(id = 9)
		@NestedFieldSetting(path = "", writeAsBytes = true, sizeField = @IntegerField(expectUniform = true, minValue = 1, maxValue = 2))
		final byte[] bytes2 = new byte[2];

		@BitField(id = 8)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final short[] shorts1 = new short[5];

		@BitField(id = 20)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final short[] shorts2 = new short[2];

		@BitField(id = 2)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final char[] chars1 = new char[5];

		@BitField(id = 15)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final char[] chars2 = new char[2];

		@BitField(id = 5)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final int[] ints1 = new int[5];

		@BitField(id = 14)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final int[] ints2 = new int[2];

		@BitField(id = 16)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final float[] floats1 = new float[5];

		@BitField(id = 19)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final float[] floats2 = new float[2];

		@BitField(id = 3)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final long[] longs1 = new long[5];

		@BitField(id = 11)
		@NestedFieldSetting(path = "", writeAsBytes = true, optional = true)
		final long[] longs2 = null;

		@BitField(id = 6)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final double[] doubles1 = new double[5];

		@BitField(id = 13)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final double[] doubles2 = new double[2];
	}

	@SuppressWarnings({"SpellCheckingInspection", "MismatchedQueryAndUpdateOfCollection"})
	@BitStruct(backwardCompatible = true)
	private static class WriteBytesAfter {

		@BitField(id = 4)
		@NestedFieldSetting(path = "", writeAsBytes = true, optional = true)
		final boolean[] bools1 = new boolean[5];

		@BitField(id = 7)
		final LinkedList<Boolean> bools2 = new LinkedList<>();

		@BitField(id = 1)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final short[] bytes1 = new short[5];

		@BitField(id = 8)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final char[] shorts1 = new char[5];

		@BitField(id = 3)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final long[] longs1 = new long[5];

		@BitField(id = 20)
		@IntegerField(expectUniform = true, minValue = -123456, maxValue = 1234567)
		final TreeSet<Long> shorts2 = new TreeSet<>();

		@BitField(id = 2)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final int[] chars1 = new int[5];

		@BitField(id = 15)
		@NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform = true))
		@IntegerField(expectUniform = false)
		final char[] chars2 = new char[15];

		@BitField(id = 5)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final float[] ints1 = new float[5];

		@BitField(id = 14)
		@IntegerField(expectUniform = true)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		final Integer[] ints2 = new Integer[100];

		@BitField(id = 9)
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		final LinkedList<Byte> bytes2 = new LinkedList<>();

		@BitField(id = 13)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final float[] doubles2 = new float[2];

		@BitField(id = 16)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final long[] floats1 = new long[5];

		@BitField(id = 19)
		@FloatField(expectMultipleOf = 5.0)
		final Double[] floats2 = new Double[0];

		@BitField(id = 11)
		@NestedFieldSetting(path = "", writeAsBytes = true, optional = true)
		byte[] longs2 = new byte[123];

		@BitField(id = 6)
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final double[] doubles1 = new double[5];
	}

	@Test
	public void testWriteAsBytesCompatibility() {
		Bitser bitser = new Bitser();
		WriteBytesBefore before = new WriteBytesBefore();
		before.bools1[1] = true;
		before.bools2[0] = true;
		before.bytes1[2] = 100;
		before.bytes2[1] = -100;
		before.shorts1[3] = 23456;
		before.shorts2[0] = Short.MIN_VALUE;
		before.chars1[4] = 'I';
		before.chars2[1] = Character.MAX_VALUE;
		before.ints1[0] = Integer.MIN_VALUE;
		before.ints2[1] = Integer.MAX_VALUE;
		before.floats1[1] = 25f;
		before.floats2[0] = -123f;
		before.longs1[2] = Long.MAX_VALUE;
		before.doubles1[3] = Double.MIN_VALUE;
		before.doubles2[1] = -Double.MAX_VALUE;

		WriteBytesAfter after = bitser.fromBytes(
				WriteBytesAfter.class,
				bitser.toBytes(before, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertArrayEquals(before.bools1, after.bools1);
		assertEquals(2, after.bools2.size());
		assertTrue(after.bools2.get(0));
		assertFalse(after.bools2.get(1));
		assertArrayEquals(new short[] { 0, 0, 100, 0, 0 }, after.bytes1);
		assertEquals(2, after.bytes2.size());
		assertEquals((byte) 0, after.bytes2.get(0));
		assertEquals((byte) -100, after.bytes2.get(1));
		assertArrayEquals(new char[] { 0, 0, 0, 23456, 0 }, after.shorts1);
		assertEquals(2, after.shorts2.size());
		assertTrue(after.shorts2.contains(0L));
		assertTrue(after.shorts2.contains((long) Short.MIN_VALUE));
		assertArrayEquals(new int[] { 0, 0, 0, 0, (int) 'I' }, after.chars1);
		assertArrayEquals(before.chars2, after.chars2);
		assertArrayEquals(new float[] { (float) Integer.MIN_VALUE, 0f, 0f, 0f, 0f }, after.ints1);
		assertArrayEquals(new Integer[] { 0, Integer.MAX_VALUE }, after.ints2);
		assertArrayEquals(new long[] { 0, 25, 0, 0, 0 }, after.floats1);
		assertArrayEquals(new Double[] { -123.0, 0.0 }, after.floats2);
		assertArrayEquals(before.longs1, after.longs1);
		assertNull(after.longs2);
		assertArrayEquals(before.doubles1, after.doubles1);
		assertArrayEquals(new float[] { 0f, Float.NEGATIVE_INFINITY }, after.doubles2);

		WriteBytesBefore back = bitser.fromBytes(
				WriteBytesBefore.class,
				bitser.toBytes(after, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertArrayEquals(before.bools1, back.bools1);
		assertArrayEquals(before.bools2, back.bools2);
		assertArrayEquals(before.bytes1, back.bytes1);
		assertArrayEquals(before.bytes2, back.bytes2);
		assertArrayEquals(before.shorts1, back.shorts1);
		assertArrayEquals(before.shorts2, back.shorts2);
		assertArrayEquals(before.chars1, back.chars1);
		assertArrayEquals(before.chars2, back.chars2);
		assertArrayEquals(before.ints1, back.ints1);
		assertArrayEquals(before.ints2, back.ints2);
		assertArrayEquals(before.floats1, back.floats1);
		assertArrayEquals(before.floats2, back.floats2);
		assertArrayEquals(before.longs1, back.longs1);
		//noinspection ConstantValue
		assertNull(back.longs2);
		assertArrayEquals(before.doubles1, back.doubles1);
		assertArrayEquals(new double[] { 0.0, Double.NEGATIVE_INFINITY }, back.doubles2);
	}

	@BitStruct(backwardCompatible = true)
	private static class BooleanArray {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final boolean[] booleans = { true, false };
	}

	@BitStruct(backwardCompatible = true)
	private static class IntArray {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		final int[] integers = { 1, 2, 3, 4 };
	}

	@BitStruct(backwardCompatible = true)
	private static class BooleanList {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final LinkedList<Boolean> booleans = new LinkedList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class IntSet {

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		@BitField(id = 0)
		@NestedFieldSetting(path = "c", optional = true)
		@IntegerField(expectUniform = true)
		final HashSet<Integer> integers = new HashSet<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class IntList {

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		@BitField(id = 0)
		@IntegerField(expectUniform = true)
		final ArrayList<Integer> integers = new ArrayList<>();
	}

	@Test
	public void testForbidBooleanArrayToIntArray() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						IntArray.class,
						bitser.toBytes(new BooleanArray(), Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "from legacy boolean true to int");
	}

	@Test
	public void testForbidBooleanArrayToIntSet() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						IntSet.class,
						bitser.toBytes(new BooleanArray(), Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "from legacy boolean true to integer");
	}

	@Test
	public void testForbidIntArrayToBooleanArray() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						BooleanArray.class,
						bitser.toBytes(new IntArray(), Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "from legacy integer 1 to boolean");
	}

	@Test
	public void testForbidIntArrayToBooleanList() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						BooleanList.class,
						bitser.toBytes(new IntArray(), Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "from legacy integer 1 to boolean");
	}

	@Test
	public void testForbidIntSetToBooleanList() {
		Bitser bitser = new Bitser();
		IntSet intSet = new IntSet();
		intSet.integers.add(1234);
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						BooleanList.class,
						bitser.toBytes(intSet, Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "from legacy integer 1234 to boolean");
	}

	@Test
	public void forbidNullsIntoPrimitives() {
		Bitser bitser = new Bitser();
		IntSet intSet = new IntSet();
		intSet.integers.add(null);
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						IntArray.class,
						bitser.toBytes(intSet, Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "IntArray.integers");
		assertContains(errorMessage, "is null");
		assertContains(errorMessage, "is no longer allowed");
	}

	@Test
	public void forbidNullsIntoNonNulls() {
		Bitser bitser = new Bitser();
		IntSet intSet = new IntSet();
		intSet.integers.add(null);
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						IntList.class,
						bitser.toBytes(intSet, Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "IntList.integers");
		assertContains(errorMessage, "is null");
		assertContains(errorMessage, "no longer allowed");
	}

	@BitStruct(backwardCompatible = true)
	private static class OldMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true)
		private static final boolean KEY_PROPERTIES = false;

		@BitField(id = 0)
		@FloatField
		@NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES")
		final TreeMap<Character, Float> map = new TreeMap<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class NewMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		private static final boolean KEY_PROPERTIES = false;

		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		@BitField(id = 0)
		@FloatField
		@NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES")
		final HashMap<Integer, Double> map = new HashMap<>();
	}

	@Test
	public void convertBetweenMaps() {
		Bitser bitser = new Bitser();
		OldMap before = new OldMap();
		before.map.put('I', 10f);
		before.map.put(Character.MAX_VALUE, 100f);

		NewMap after = bitser.fromBytes(
				NewMap.class,
				bitser.toBytes(before, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(2, after.map.size());
		assertEquals(10.0, after.map.get((int) 'I'));
		assertEquals(100.0, after.map.get((int) Character.MAX_VALUE));

		OldMap back = bitser.fromBytes(
				OldMap.class,
				bitser.toBytes(after, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(before.map, back.map);
	}

	@BitStruct(backwardCompatible = true)
	private static class StringList {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final ArrayList<String> strings = new ArrayList<>();
	}

	@Test
	public void testForbidIntListToStringList() {
		Bitser bitser = new Bitser();
		IntList intList = new IntList();
		intList.integers.add(1234);

		String errorMessage = assertThrows(
				LegacyBitserException.class, () -> bitser.fromBytes(
						StringList.class,
						bitser.toBytes(intList, Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "convert from legacy integer 1234");
		assertContains(errorMessage, "String");
		assertContains(errorMessage, "StringList.strings");
	}

	@BitStruct(backwardCompatible = true)
	private static class JustID {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testForbidUuidToIntArray() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(
						IntArray.class, bitser.toBytes(new JustID(), Bitser.BACKWARD_COMPATIBLE),
						Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "convert from legacy ");
		assertContains(errorMessage, "int[]");
		assertContains(errorMessage, "IntArray.integers");
	}

	@BitStruct(backwardCompatible = true)
	private static class StructList {

		@BitField(id = 0)
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		final ArrayList<JustID> theList = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceList {

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "references")
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		final ArrayList<JustID> theList = new ArrayList<>();

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "references")
		final ArrayList<JustID> theTargets = new ArrayList<>();
	}

	@Test
	public void testInvalidStructListToReferenceList() {
		Bitser bitser = new Bitser();

		StructList structList = new StructList();
		structList.theList.add(new JustID());
		byte[] structBytes = bitser.toBytes(structList, Bitser.BACKWARD_COMPATIBLE);

		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(ReferenceList.class, structBytes, Bitser.BACKWARD_COMPATIBLE)
		).getMessage();
		assertContains(errorMessage, "ReferenceList -> theList");
		assertContains(errorMessage, "from legacy LegacyStructInstance to reference");
	}

	@Test
	public void testInvalidReferenceListToStructList() {
		Bitser bitser = new Bitser();

		ReferenceList referenceList = new ReferenceList();
		referenceList.theTargets.add(new JustID());
		referenceList.theList.add(referenceList.theTargets.get(0));
		byte[] referenceBytes = bitser.toBytes(referenceList, Bitser.BACKWARD_COMPATIBLE);

		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(StructList.class, referenceBytes, Bitser.BACKWARD_COMPATIBLE)
		).getMessage();
		assertContains(errorMessage, "StructList -> theList");
		assertContains(errorMessage, "from legacy reference to LegacyStructInstance");
	}
}
