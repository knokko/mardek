package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestNestedFieldSetting {

	@BitStruct(backwardCompatible = false)
	static class AlternatingOptionalCollection {

		@FloatField(expectMultipleOf = 0.5)
		@NestedFieldSetting(path = "cc", optional = true)
		@NestedFieldSetting(path = "ccc", optional = true)
		ArrayList<HashSet<HashSet<LinkedList<Float>>>> nested = new ArrayList<>();

		@BitField
		String test;
	}

	@Test
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public void testAlternatingDepthOptionalCollection() {
		LinkedList<Float> floatList = new LinkedList<>();
		floatList.add(1f);
		floatList.add(5f);

		HashSet<LinkedList<Float>> listSet = new HashSet<>();
		listSet.add(floatList);
		listSet.add(null);

		HashSet<HashSet<LinkedList<Float>>> hashSet = new HashSet<>();
		hashSet.add(null);
		hashSet.add(listSet);

        AlternatingOptionalCollection alternating = new AlternatingOptionalCollection();
		alternating.nested = new ArrayList<>();
		alternating.nested.add(hashSet);
		alternating.test = "test1234";

		AlternatingOptionalCollection loaded = new Bitser().stupidDeepCopy(alternating);
		assertEquals("test1234", loaded.test);
		assertEquals(1, loaded.nested.size());

		HashSet<HashSet<LinkedList<Float>>> loadedHashSet = loaded.nested.get(0);
		assertEquals(2, loadedHashSet.size());
		assertTrue(loadedHashSet.contains(null));

		HashSet<LinkedList<Float>> loadedListSet = loadedHashSet.stream().filter(Objects::nonNull).findAny().get();
		assertEquals(2, loadedListSet.size());
		assertTrue(loadedListSet.contains(null));

		LinkedList<Float> loadedLinkedList = loadedListSet.stream().filter(Objects::nonNull).findAny().get();
		assertEquals(2, loadedLinkedList.size());
		assertTrue(loadedLinkedList.contains(1f));
		assertTrue(loadedLinkedList.contains(5f));
	}

	@BitStruct(backwardCompatible = false)
	static class AlternatingOptionalArray {

		@FloatField(expectMultipleOf = 0.5)
		@NestedFieldSetting(path = "cc", optional = true)
		@NestedFieldSetting(path = "ccc", optional = true)
		float[][][][] nested;

		@BitField
		String test;
	}

	@Test
	public void testAlternatingOptionalArray() {
		AlternatingOptionalArray alternating = new AlternatingOptionalArray();
		float[][][][] nested = {
				{
						null,
						{
								null,
								{ 3f }
						}
				}
		};
		alternating.nested = nested;
		alternating.test = "ok";

		AlternatingOptionalArray loaded = new Bitser().stupidDeepCopy(alternating);
		assertNotSame(nested, loaded.nested);
		assertEquals(1, loaded.nested.length);
		assertEquals(2, loaded.nested[0].length);
		assertNull(loaded.nested[0][0]);
		assertEquals(2, loaded.nested[0][1].length);
		assertNull(loaded.nested[0][1][0]);
		assertEquals(1, loaded.nested[0][1][1].length);
		assertEquals(3f, loaded.nested[0][1][1][0], 1e-4f);
	}

	@BitStruct(backwardCompatible = false)
	static class AlternatingMadness {

		@IntegerField(expectUniform = true, minValue = 10, maxValue = 15)
		@NestedFieldSetting(path = "", optional = true)
		@NestedFieldSetting(path = "c", optional = true)
		ArrayList<HashSet<LinkedList<int[]>>[]> nested;
	}

	@Test
	public void testAlternatingMadnessNull() {
		assertNull(new Bitser().stupidDeepCopy(new AlternatingMadness()).nested);
	}

	@Test
	public void testAlternatingMadness() {
		LinkedList<int[]> list = new LinkedList<>();
		list.add(new int[] { 11, 12 });

		HashSet<LinkedList<int[]>> set = new HashSet<>();
		set.add(list);
		@SuppressWarnings("unchecked")
		HashSet<LinkedList<int[]>>[] setArray = new HashSet[1];
		setArray[0] = set;

		AlternatingMadness alternating = new AlternatingMadness();
		alternating.nested = new ArrayList<>();
		alternating.nested.add(null);
		alternating.nested.add(setArray);

		AlternatingMadness loaded = new Bitser().stupidDeepCopy(alternating);
		assertEquals(2, loaded.nested.size());
		assertNull(loaded.nested.get(0));
		assertEquals(1, loaded.nested.get(1).length);

		HashSet<LinkedList<int[]>> loadedSet = loaded.nested.get(1)[0];
		assertEquals(1, loadedSet.size());
		LinkedList<int[]> loadedList = loadedSet.iterator().next();
		assertEquals(1, loadedList.size());

		assertArrayEquals(new int[] { 11, 12 }, loadedList.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class WithCustomSizes {

		@NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform = true, minValue = 1, maxValue = 2))
		@NestedFieldSetting(path = "c", sizeField = @IntegerField(expectUniform = true, minValue = 0, maxValue = 15))
		final ArrayList<LinkedList<Boolean>> list = new ArrayList<>();
	}

	@Test
	public void testWithCustomSizes() throws IOException {
		LinkedList<Boolean> innerList = new LinkedList<>();
		innerList.add(true);
		innerList.add(true);
		WithCustomSizes sizes = new WithCustomSizes();
		sizes.list.add(innerList);

		WithCustomSizes loaded = new Bitser().stupidDeepCopy(sizes);
		assertEquals(1, loaded.list.size());
		LinkedList<Boolean> loadedList = loaded.list.get(0);
		assertEquals(2, loadedList.size());
		assertTrue(loadedList.get(0));
		assertTrue(loadedList.get(1));

		// 1 bit to store the size of the outer list
		// 4 bits to store the size of the inner list
		// 2 bits to store the boolean values
		BitCountStream counter = new BitCountStream();
		new Bitser().serialize(sizes, counter);
		assertEquals(7, counter.getCounter());
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceLists1 {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean TARGET_LIST = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean REFERENCE_LIST = false;

		@NestedFieldSetting(path = "c", fieldName = "TARGET_LIST")
		final LinkedList<LinkedList<String>> targetList = new LinkedList<>();

		@NestedFieldSetting(path = "", fieldName = "REFERENCE_LIST")
		LinkedList<String> referenceList;
	}

	@Test
	public void testReferenceLists1() {
		LinkedList<String> list1 = new LinkedList<>();
		list1.add("hello");

		LinkedList<String> list2 = new LinkedList<>();
		list2.add("world");

		ReferenceLists1 referenceLists = new ReferenceLists1();
		referenceLists.targetList.add(list1);
		referenceLists.targetList.add(list2);
		referenceLists.referenceList = list2;

		ReferenceLists1 loaded = new Bitser().stupidDeepCopy(referenceLists);
		assertEquals(2, loaded.targetList.size());
		assertEquals(1, loaded.targetList.get(0).size());
		assertEquals("hello", loaded.targetList.get(0).get(0));
		assertEquals(1, loaded.targetList.get(1).size());
		assertEquals("world", loaded.targetList.get(1).get(0));
		assertSame(loaded.targetList.get(1), loaded.referenceList);
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceLists2 {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean TARGET_LIST = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean REFERENCE_LIST = false;

		@NestedFieldSetting(path = "", fieldName = "TARGET_LIST")
		final LinkedList<String> targetList = new LinkedList<>();

		@NestedFieldSetting(path = "c", fieldName = "REFERENCE_LIST")
		final LinkedList<LinkedList<String>> referenceList = new LinkedList<>();
	}

	@Test
	public void testReferenceLists2() {
		ReferenceLists2 referenceLists = new ReferenceLists2();
		referenceLists.targetList.add("hello");
		referenceLists.referenceList.add(referenceLists.targetList);

		ReferenceLists2 loaded = new Bitser().stupidDeepCopy(referenceLists);
		assertEquals(1, loaded.targetList.size());
		assertEquals("hello", loaded.targetList.get(0));

		assertEquals(1, loaded.referenceList.size());
		assertSame(loaded.targetList, loaded.referenceList.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceLists3 {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean TARGET = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean REFERENCE = false;

		@NestedFieldSetting(path = "c", fieldName = "TARGET")
		final LinkedList<String> targetList1 = new LinkedList<>();

		@ReferenceFieldTarget(label = "test")
		final LinkedList<String> targetList2 = new LinkedList<>();

		@NestedFieldSetting(path = "cc", fieldName = "REFERENCE")
		final LinkedList<LinkedList<String>> referenceList1 = new LinkedList<>();

		@ReferenceField(stable = false, label = "test")
		final LinkedList<String> referenceList2 = new LinkedList<>();
	}

	@Test
	public void testReferenceLists3() {
		ReferenceLists3 referenceLists = new ReferenceLists3();
		referenceLists.targetList1.add("hello");
		referenceLists.targetList2.add("world");
		referenceLists.referenceList1.add(new LinkedList<>());
		referenceLists.referenceList1.get(0).add(referenceLists.targetList1.get(0));
		referenceLists.referenceList1.get(0).add(referenceLists.targetList2.get(0));
		referenceLists.referenceList2.add(referenceLists.targetList1.get(0));

		ReferenceLists3 loaded = new Bitser().stupidDeepCopy(referenceLists);
		assertEquals(1, loaded.targetList1.size());
		assertEquals("hello", loaded.targetList1.get(0));
		assertEquals(1, loaded.targetList2.size());
		assertEquals("world", loaded.targetList2.get(0));

		assertEquals(1, loaded.referenceList1.size());
		assertEquals(2, loaded.referenceList1.get(0).size());
		assertSame(loaded.targetList1.get(0), loaded.referenceList1.get(0).get(0));
		assertSame(loaded.targetList2.get(0), loaded.referenceList1.get(0).get(1));

		assertEquals(1, loaded.referenceList2.size());
		assertEquals(loaded.targetList1.get(0), loaded.referenceList2.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class DuplicateNestedFieldSettings1 {

		@SuppressWarnings("unused")
		@NestedFieldSetting(path = "")
		@NestedFieldSetting(path = "")
		final ArrayList<LinkedList<String>> badList = new ArrayList<>();
	}

	@Test
	public void testDuplicateNestedFieldSettings1() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new DuplicateNestedFieldSettings1(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Multiple NestedFieldSetting's for path ");
	}

	@BitStruct(backwardCompatible = false)
	static class DuplicateNestedFieldSettings2 {

		@SuppressWarnings("unused")
		@NestedFieldSetting(path = "c", writeAsBytes = true)
		@NestedFieldSetting(path = "cc")
		@NestedFieldSetting(path = "cc")
		final ArrayList<LinkedList<Byte>> badList = new ArrayList<>();
	}

	@Test
	public void testDuplicateNestedFieldSettings2() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new DuplicateNestedFieldSettings2(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Multiple NestedFieldSetting's for path cc");
	}

	@BitStruct(backwardCompatible = false)
	static class OptionalBitField {

		@SuppressWarnings("unused")
		@BitField(optional = true)
		final ArrayList<String> test = new ArrayList<>();
	}

	@Test
	public void testForbidOptionalBitFields() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new OptionalBitField(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "optional BitField is not allowed on collection field");
		assertContains(errorMessage, "use @NestedFieldSetting instead");
	}

	@Test
	public void testInvalidAlternatingOptionalCollections() {
		AlternatingOptionalCollection alternating = new AlternatingOptionalCollection();
		alternating.test = "test";
		alternating.nested.add(null);

		String error1 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(alternating, new BitCountStream())
		).getMessage();
		assertContains(error1, "must not have null elements");

		LinkedList<Float> floatList = new LinkedList<>();
		floatList.add(null);
		floatList.add(5f);

		HashSet<LinkedList<Float>> listSet = new HashSet<>();
		listSet.add(floatList);

		HashSet<HashSet<LinkedList<Float>>> hashSet = new HashSet<>();
		hashSet.add(listSet);

		alternating.nested.clear();
		alternating.nested.add(hashSet);

		String error2 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(alternating, new BitCountStream())
		).getMessage();
		assertContains(error2, "must not have null elements");

		alternating.nested = null;
		String error3 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(alternating, new BitCountStream())
		).getMessage();
		assertContains(error3, "must not be null");
	}

	@Test
	public void testInvalidAlternatingOptionalArray() {
		AlternatingOptionalArray alternating = new AlternatingOptionalArray();
		alternating.test = "test";
		alternating.nested = new float[][][][] {
				null
		};

		String error1 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(alternating, new BitCountStream())
		).getMessage();
		assertContains(error1, "must not have null elements");

		alternating.nested = null;
		String error2 = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(alternating, new BitCountStream())
		).getMessage();
		assertContains(error2, "must not be null");
	}

	@BitStruct(backwardCompatible = true)
	private static class MonsterCollection {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true)
		private static final boolean KEY_PROPERTIES = false;

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		private static final boolean VALUE_PROPERTIES = false;

		@BitField(id = 0)
		@NestedFieldSetting(path = "kc", fieldName = "KEY_PROPERTIES")
		@NestedFieldSetting(path = "vcc", optional = true)
		@NestedFieldSetting(path = "vccc", fieldName = "VALUE_PROPERTIES")
		final HashMap<ArrayList<Short>, HashSet<TreeSet<Integer>>[]> root = new HashMap<>();
	}

	private void checkNestedMonster(MonsterCollection monster, ArrayList<Short> key, TreeSet<Integer> innerSet) {
		assertEquals(1, monster.root.size());

		assertEquals(1, monster.root.get(key).length);
		HashSet<TreeSet<Integer>>highValue = monster.root.get(key)[0];
		assertEquals(2, highValue.size());
		assertTrue(highValue.contains(null));
		assertTrue(highValue.contains(innerSet));
	}

	@Test
	public void testNestedMonster() {
		Bitser bitser = new Bitser();
		MonsterCollection monster = new MonsterCollection();
		ArrayList<Short> key = new ArrayList<>();
		key.add((short) -12345);

		HashSet<TreeSet<Integer>> highValue = new HashSet<>();
		//noinspection unchecked
		monster.root.put(key, new HashSet[] { highValue });

		highValue.add(null);
		TreeSet<Integer> innerSet = new TreeSet<>();
		highValue.add(innerSet);
		innerSet.add(321);

		checkNestedMonster(bitser.stupidDeepCopy(monster), key, innerSet);
		checkNestedMonster(bitser.stupidDeepCopy(monster, Bitser.BACKWARD_COMPATIBLE), key, innerSet);
	}
}
