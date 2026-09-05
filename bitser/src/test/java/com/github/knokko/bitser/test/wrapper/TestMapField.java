package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestMapField {

	@BitStruct(backwardCompatible = false)
	static class StringMap {

		@BitField
		final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
	}

	@Test
	public void testStringMap() {
		Bitser bitser = new Bitser();
		assertEquals(0, bitser.stupidDeepCopy(new StringMap()).map.size());

		StringMap stringMap = new StringMap();
		stringMap.map.put("hello", "world");
		stringMap.map.put("hi", "triangle");

		StringMap loaded = bitser.stupidDeepCopy(stringMap);
		assertEquals(2, loaded.map.size());
		assertEquals("world", loaded.map.get("hello"));
		assertEquals("triangle", loaded.map.get("hi"));
	}

	@BitStruct(backwardCompatible = false)
	static class IntMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true, minValue = 10, maxValue = 20)
		private static final boolean KEY = false;

		@IntegerField(expectUniform = true, minValue = 50, maxValue = 60)
		@NestedFieldSetting(path = "", sizeField = @IntegerField(expectUniform = true, minValue = 0, maxValue = 3))
		@NestedFieldSetting(path = "k", fieldName = "KEY")
		TreeMap<Integer, Byte> map;
	}

	@Test
	public void testIntMap() throws IOException {
		Bitser bitser = new Bitser();
		IntMap intMap = new IntMap();
		intMap.map = new TreeMap<>();
		intMap.map.put(10, (byte) 50);

		IntMap loaded = bitser.stupidDeepCopy(intMap);
		assertEquals(1, loaded.map.size());
		assertEquals((byte) 50, loaded.map.get(10));

		// 2 bits for the size
		// 4 bits for the key
		// 4 bits for the value
		BitCountStream counter = new BitCountStream();
		bitser.serialize(intMap, counter);
		assertEquals(10, counter.getCounter());
	}

	@Test
	public void testIntMapDeepEqualsAndHashCode() {
		IntMap a = new IntMap();
		IntMap b = new IntMap();

		Bitser bitser = new Bitser();
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.map = new TreeMap<>();
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.map = new TreeMap<>();
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.map.put(12, (byte) 34);
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.map.put(12, (byte) 45);
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.map.put(12, (byte) 34);
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.map.put(51, (byte) 100);
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.map.put(52, (byte) 100);
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.map.put(51, (byte) 100);
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.map.remove(52);
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));
	}

	@BitStruct(backwardCompatible = false)
	static class KeyFieldMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false, minValue = 100)
		private static final boolean KEY = false;

		@NestedFieldSetting(path = "k", optional = true, fieldName = "KEY")
		final HashMap<Integer, String> map = new HashMap<>();
	}

	@Test
	public void testKeyFieldMap() {
		KeyFieldMap keyMap = new KeyFieldMap();
		keyMap.map.put(null, "hello");
		keyMap.map.put(150, "world");

		KeyFieldMap loaded = new Bitser().stupidDeepCopy(keyMap);
		assertEquals(2, loaded.map.size());
		assertEquals("hello", loaded.map.get(null));
		assertEquals("world", loaded.map.get(150));
	}

	@BitStruct(backwardCompatible = false)
	static class ImplicitValueFieldMap {

		@IntegerField(expectUniform = true, minValue = 10, maxValue = 11)
		final HashMap<String, Integer> map = new HashMap<>();
	}

	@Test
	public void testImplicitValueFieldMap() {
		ImplicitValueFieldMap valueMap = new ImplicitValueFieldMap();
		valueMap.map.put("price", 11);
		valueMap.map.put("weight", 10);

		ImplicitValueFieldMap loaded = new Bitser().stupidDeepCopy(valueMap);
		assertEquals(2, loaded.map.size());
		assertEquals(11, loaded.map.get("price"));
		assertEquals(10, loaded.map.get("weight"));
	}

	@BitStruct(backwardCompatible = false)
	static class ExplicitValueFieldMap {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true, minValue = 10, maxValue = 11)
		private static final boolean VALUE = false;

		@NestedFieldSetting(path = "v", fieldName = "VALUE", optional = true)
		final TreeMap<String, Integer> map = new TreeMap<>();
	}

	@Test
	public void testExplicitValueFieldMap() {
		ExplicitValueFieldMap valueMap = new ExplicitValueFieldMap();
		valueMap.map.put("knokko", 10);
		valueMap.map.put("knok", null);

		ExplicitValueFieldMap loaded = new Bitser().stupidDeepCopy(valueMap);
		assertEquals(2, loaded.map.size());
		assertEquals(10, loaded.map.get("knokko"));
		assertTrue(loaded.map.containsKey("knok"));
		assertNull(loaded.map.get("knok"));
	}

	@BitStruct(backwardCompatible = false)
	static class OptionalMap {

		@NestedFieldSetting(path = "", optional = true)
		TreeMap<String, String> map;
	}

	@Test
	public void testOptionalMap() {
		assertNull(new Bitser().stupidDeepCopy(new OptionalMap()).map);
	}

	@BitStruct(backwardCompatible = false)
	static class ReferenceMaps {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "test")
		private static final boolean KEY1 = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		private static final boolean KEY2 = true;

		@SuppressWarnings("unused")
		@ReferenceField(stable = true, label = "test")
		private static final boolean VALUE = true;

		@NestedFieldSetting(path = "k", fieldName = "KEY1")
		@NestedFieldSetting(path = "v", fieldName = "VALUE")
		final ConcurrentHashMap<ReferenceMaps, ReferenceMaps> map1 = new ConcurrentHashMap<>();

		@NestedFieldSetting(path = "k", fieldName = "KEY2")
		@ReferenceFieldTarget(label = "test")
		final ConcurrentHashMap<ReferenceMaps, ReferenceMaps> map2 = new ConcurrentHashMap<>();

		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testReferenceMaps() {
		ReferenceMaps inner1 = new ReferenceMaps();
		ReferenceMaps inner2 = new ReferenceMaps();
		ReferenceMaps inner3 = new ReferenceMaps();
		inner2.map1.put(inner3, inner2);
		inner2.map2.put(inner1, inner1);

		ReferenceMaps outer = new ReferenceMaps();
		outer.map2.put(inner1, inner2);

		ReferenceMaps loaded = new Bitser().stupidDeepCopy(outer);
		assertEquals(0, loaded.map1.size());
		assertEquals(1, loaded.map2.size());

		ReferenceMaps loaded1 = loaded.map2.keys().nextElement();
		assertEquals(0, loaded1.map1.size());
		assertEquals(0, loaded1.map2.size());

		ReferenceMaps loaded2 = loaded.map2.get(loaded1);
		assertEquals(1, loaded2.map1.size());
		assertEquals(1, loaded2.map2.size());

		assertSame(loaded2, loaded2.map1.values().iterator().next());
		assertNotSame(loaded1, loaded2.map1.keys().nextElement());
		assertSame(loaded1, loaded2.map2.get(loaded1));

		assertEquals(outer.id, loaded.id);
		assertEquals(inner1.id, loaded1.id);
		assertEquals(inner2.id, loaded2.id);
	}

	@BitStruct(backwardCompatible = false)
	static class WriteAsBytesMap {

		@SuppressWarnings("unused")
		@NestedFieldSetting(path = "", writeAsBytes = true)
		final TreeMap<Byte, Byte> map = new TreeMap<>();
	}

	@Test
	public void testWriteAsBytesMap() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new WriteAsBytesMap(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "writeAsBytes is not allowed on Maps");
	}

	@Test
	public void testNullKeysAreForbiddenByDefault() {
		ImplicitValueFieldMap valueMap = new ImplicitValueFieldMap();
		valueMap.map.put(null, 11);

		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(valueMap, new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "must not have null keys");
	}

	@Test
	public void testNullValuesAreForbiddenByDefault() {
		KeyFieldMap keyMap = new KeyFieldMap();
		keyMap.map.put(1234, null);

		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(keyMap, new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "must not have null values");
	}

	@Test
	public void testNullMapsAreForbiddenByDefault() {
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().serialize(new IntMap(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "must not be null");
	}

	@BitStruct(backwardCompatible = false)
	static class BadOptionalMap {

		@SuppressWarnings("unused")
		@BitField(optional = true)
		HashMap<String, String> map;
	}

	@Test
	public void testBadOptionalMap() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new BadOptionalMap(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "optional BitField is not allowed");
		assertContains(errorMessage, "use @NestedFieldSetting instead");
	}

	@BitStruct(backwardCompatible = true)
	private static class MiniStruct {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int size;
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceToStructMap {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "names")
		private static final boolean KEY_PROPERTIES = false;

		@BitField(id = 0)
		@NestedFieldSetting(path = "k", fieldName = "KEY_PROPERTIES")
		final HashMap<String, MiniStruct> map = new HashMap<>();

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "names")
		String name;
	}

	@Test
	public void testReferenceToStructMap() {
		MiniStruct mini = new MiniStruct();
		mini.size = 3;

		ReferenceToStructMap original = new ReferenceToStructMap();
		original.name = "Tim";
		original.map.put(original.name, mini);

		ReferenceToStructMap copy = new Bitser().stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("Tim", copy.name);
		assertEquals(3, copy.map.get(copy.name).size);
	}

	@Test
	public void testReferenceToStructDeepEqualsAndHashCode() {
		MiniStruct miniA = new MiniStruct();
		miniA.size = 12;

		MiniStruct miniB = new MiniStruct();
		miniB.size = 12;

		String keyA = "key";
		@SuppressWarnings("StringOperationCanBeSimplified")
		String keyB = new String(keyA);

		ReferenceToStructMap a = new ReferenceToStructMap();
		a.map.put(keyA, miniA);

		ReferenceToStructMap b = new ReferenceToStructMap();
		b.map.put(keyB, miniB);

		Bitser bitser = new Bitser();
		assertFalse(bitser.deepEquals(a, b));

		b.map.remove(keyB);
		b.map.put(keyA, miniB);
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		miniB.size = 20;
		assertFalse(bitser.deepEquals(a, b));
	}
}
