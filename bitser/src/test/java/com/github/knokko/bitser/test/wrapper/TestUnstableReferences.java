package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitPostInit;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.legacy.LegacyReference;
import com.github.knokko.bitser.options.WithParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestUnstableReferences {

	@BitStruct(backwardCompatible = true)
	private static class ItemType {

		@BitField(id = 0)
		final String name;

		@SuppressWarnings("unused")
		ItemType() {
			this(null);
		}

		ItemType(String name) {
			this.name = name;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class Item {

		@StringField
		final String name;

		@ReferenceField(stable = false, label = "item types")
		final ItemType type;

		@SuppressWarnings("unused")
		Item() {
			this(null, null);
		}

		Item(String name, ItemType type) {
			this.name = name;
			this.type = type;
		}
	}

	@BitStruct(backwardCompatible = false)
	private static class ItemRoot {

		@BitField
		final ArrayList<Item> items = new ArrayList<>();

		@ReferenceFieldTarget(label = "item types")
		final ArrayList<ItemType> types = new ArrayList<>();
	}

	@Test
	public void testItems() {

		ItemRoot root = new ItemRoot();
		ItemType sword = new ItemType("sword");
		ItemType shield = new ItemType("shield");

		root.types.add(sword);
		root.types.add(shield);

		root.items.add(new Item("rusty sword", sword));
		root.items.add(new Item("cold shield", shield));
		root.items.add(new Item("spiky shield", shield));

		ItemRoot loaded = new Bitser().stupidDeepCopy(root);

		assertEquals(2, loaded.types.size());
		assertEquals("sword", loaded.types.get(0).name);
		assertEquals("shield", loaded.types.get(1).name);
		assertEquals(3, loaded.items.size());
		assertEquals("rusty sword", loaded.items.get(0).name);
		assertEquals("cold shield", loaded.items.get(1).name);
		assertEquals("spiky shield", loaded.items.get(2).name);
		assertSame(loaded.types.get(0), loaded.items.get(0).type);
		assertSame(loaded.types.get(1), loaded.items.get(1).type);
		assertSame(loaded.types.get(1), loaded.items.get(2).type);
	}

	@BitStruct(backwardCompatible = false)
	static class Node {

		@IntegerField(minValue = 1, expectUniform = false)
		final int score;

		@ReferenceField(stable = false, label = "nodes")
		final ArrayList<Node> neighbours = new ArrayList<>();

		@BitField(optional = true)
		DeepNodeReference bestFriend;

		Node(int score) {
			this.score = score;
		}

		@SuppressWarnings("unused")
		Node() {
			this(0);
		}
	}

	@BitStruct(backwardCompatible = false)
	static class DeepNodeReference {

		@ReferenceField(stable = false, label = "nodes")
		final Node node;

		DeepNodeReference(Node node) {
			this.node = node;
		}

		@SuppressWarnings("unused")
		DeepNodeReference() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = false)
	static class DeepNodeTarget {

		@ReferenceFieldTarget(label = "nodes")
		final Node node;

		DeepNodeTarget(Node node) {
			this.node = node;
		}

		@SuppressWarnings("unused")
		DeepNodeTarget() {
			this(null);
		}
	}

	@BitStruct(backwardCompatible = false)
	static class Graph {

		@ReferenceFieldTarget(label = "nodes")
		final ArrayList<Node> mostNodes = new ArrayList<>();

		@BitField
		DeepNodeReference champion;

		@BitField
		DeepNodeTarget loser;
	}

	@Test
	public void testGraph() {
		Graph graph = new Graph();

		Node champion = new Node(100);
		Node loser = new Node(1);
		Node node1 = new Node(10);
		Node node2 = new Node(20);
		Node node3 = new Node(30);

		graph.mostNodes.add(node1);
		graph.mostNodes.add(node2);
		graph.mostNodes.add(node3);
		graph.mostNodes.add(champion);
		graph.champion = new DeepNodeReference(champion);
		graph.loser = new DeepNodeTarget(loser);

		champion.neighbours.add(node2);
		node2.neighbours.add(champion);
		node2.neighbours.add(node3);
		node1.bestFriend = new DeepNodeReference(loser);
		node3.bestFriend = new DeepNodeReference(node2);
		node3.neighbours.add(node1);

		Graph loaded = new Bitser().stupidDeepCopy(graph);
		assertEquals(4, loaded.mostNodes.size());

		Node loaded1 = loaded.mostNodes.get(0);
		Node loaded2 = loaded.mostNodes.get(1);
		Node loaded3 = loaded.mostNodes.get(2);
		Node loadedChampion = loaded.mostNodes.get(3);
		Node loadedLoser = loaded.loser.node;

		// Sanity checks
		assertNotSame(loaded1, loaded2);
		assertNotSame(loaded2, loaded3);
		assertNotSame(loadedChampion, loadedLoser);
		assertSame(loadedChampion, loaded.champion.node);

		assertEquals(100, loadedChampion.score);
		assertEquals(1, loadedLoser.score);
		assertEquals(10, loaded1.score);
		assertEquals(20, loaded2.score);
		assertEquals(30, loaded3.score);

		assertSame(loaded1.bestFriend.node, loadedLoser);
		assertSame(loaded3.bestFriend.node, loaded2);

		assertEquals(2, loaded2.neighbours.size());
		assertSame(loadedChampion, loaded2.neighbours.get(0));
		assertSame(loaded3, loaded2.neighbours.get(1));
		assertEquals(1, loadedChampion.neighbours.size());
		assertSame(loaded2, loadedChampion.neighbours.get(0));
		assertEquals(1, loaded3.neighbours.size());
		assertSame(loaded1, loaded3.neighbours.get(0));
	}

	@BitStruct(backwardCompatible = false)
	static class NonStructReferences {

		@ReferenceFieldTarget(label = "no struct")
		final String hello;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "no struct")
		final String hi;

		NonStructReferences(String hello) {
			this.hello = hello;
			this.hi = this.hello;
		}

		@SuppressWarnings("unused")
		NonStructReferences() {
			this(null);
		}
	}

	@Test
	public void testNonBitStructReferences() {
		NonStructReferences loaded = new Bitser().stupidDeepCopy(new NonStructReferences("world"));
		assertEquals("world", loaded.hello);
		assertSame(loaded.hello, loaded.hi);
		assertTrue(new Bitser().deepEquals(loaded, new NonStructReferences("world")));
	}

	@Test
	public void testReferenceOutsideRoot() {
		ItemRoot root = new ItemRoot();
		root.items.add(new Item("item", new ItemType("outside")));

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> new Bitser().serialize(root, new BitCountStream())
		).getMessage();

		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label item types");
	}

	@BitStruct(backwardCompatible = false)
	static class MissingTargetLabel {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "nope")
		final String reference = "hello";
	}

	@Test
	public void testMissingTargetLabel() {
		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> new Bitser().serialize(new MissingTargetLabel(), new BitCountStream())
		).getMessage();

		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label nope");
	}

	@BitStruct(backwardCompatible = false)
	static class BothReferenceAndTarget {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "test")
		@ReferenceFieldTarget(label = "test")
		final JustAnId id = new JustAnId();
	}

	@BitStruct(backwardCompatible = false)
	static class JustAnId {

		@SuppressWarnings("unused")
		@BitField
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testFieldThatIsBothReferenceAndTarget() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new BothReferenceAndTarget(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "is both a reference field and a reference target");
	}

	@BitStruct(backwardCompatible = false)
	static class WithPrimitiveTarget {

		@SuppressWarnings("unused")
		@IntegerField(expectUniform = false)
		@ReferenceFieldTarget(label = "invalid")
		final int test = 1234;
	}

	@Test
	public void testPrimitiveTarget() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new WithPrimitiveTarget(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "is primitive, which is forbidden");
	}

	@Test
	public void testMultipleTargetsWithTheSameIdentity() {
		ItemRoot root = new ItemRoot();
		ItemType type = new ItemType("dual");
		root.types.add(type);
		root.types.add(type);

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> new Bitser().serialize(root, new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Multiple unstable targets have identity");
		assertContains(errorMessage, "ItemRoot -> types");
	}

	@BitStruct(backwardCompatible = false)
	static class StableTarget {

		@SuppressWarnings("unused")
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();

		@BitField
		String hello;
	}

	@BitStruct(backwardCompatible = false)
	static class Mixed {

		@ReferenceFieldTarget(label = "hi")
		StableTarget target1;

		@ReferenceFieldTarget(label = "hi")
		StableTarget target2;

		@ReferenceField(stable = true, label = "hi")
		StableTarget stableReference;

		@ReferenceField(stable = false, label = "hi")
		StableTarget unstableReference1;

		@ReferenceField(stable = false, label = "hi")
		StableTarget unstableReference2;
	}

	@Test
	public void testMixStableAndUnstableReferences() {
		Mixed mixed = new Mixed();
		mixed.target1 = new StableTarget();
		mixed.target1.hello = "world";
		mixed.target2 = new StableTarget();
		mixed.target2.hello = "triangle";

		mixed.stableReference = mixed.target2;
		mixed.unstableReference1 = mixed.target1;
		mixed.unstableReference2 = mixed.target2;

		Mixed loaded = new Bitser().stupidDeepCopy(mixed);
		assertEquals("world", loaded.target1.hello);
		assertEquals("triangle", loaded.target2.hello);
		assertSame(loaded.target1, loaded.unstableReference1);
		assertSame(loaded.target2, loaded.stableReference);
		assertSame(loaded.target2, loaded.unstableReference2);
		assertTrue(new Bitser().deepEquals(mixed, loaded));
		assertEquals(new Bitser().hashCode(mixed), new Bitser().hashCode(loaded));
	}

	@Test
	public void testMissingLegacyTargetLabel1() {
		ItemType itemType = new ItemType("bye bye");

		ItemRoot primaryRoot = new ItemRoot();
		primaryRoot.items.add(new Item("it", itemType));

		ItemRoot withRoot = new ItemRoot();
		withRoot.types.add(itemType);

		Bitser bitser = new Bitser();
		byte[] bytes = bitser.toBytes(primaryRoot, withRoot);

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> bitser.fromBytes(ItemRoot.class, bytes)
		).getMessage();
		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label item types");
		assertContains(errorMessage, "-> type");
	}

	@Test
	public void testMissingLegacyTargetLabel2() {
		ItemType itemType = new ItemType("bye bye");

		Item item = new Item("it", itemType);

		ItemRoot with = new ItemRoot();
		with.types.add(itemType);

		Bitser bitser = new Bitser();
		byte[] bytes = bitser.toBytes(item, with);

		String errorMessage = assertThrows(
				ReferenceBitserException.class,
				() -> bitser.fromBytes(Item.class, bytes)
		).getMessage();
		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label item types");
		assertContains(errorMessage, "-> type");
	}

	@BitStruct(backwardCompatible = true)
	private static class RequiredReferenceList {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "label")
		final String target = "abc";

		@BitField(id = 1)
		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "label")
		String[] references = { target, null };
	}

	@Test
	public void testForbidNullReferences() {
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().toBytes(new RequiredReferenceList())
		).getMessage();
		assertContains(errorMessage, "RequiredReferenceList -> references");
		assertContains(errorMessage, "must not have null elements");
	}

	@BitStruct(backwardCompatible = true)
	private static class GenericTarget<T> {

		@SuppressWarnings("unused")
		T defaultValue;
	}

	@BitStruct(backwardCompatible = true)
	private static class GenericRoot {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "generic1")
		GenericTarget<?> target;

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "generic1")
		GenericTarget<?> reference;

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "generic2")
		GenericTarget<?>[] targets;

		@BitField(id = 3)
		@ReferenceField(stable = false, label = "generic2")
		GenericTarget<?>[] references;

		@BitField(id = 4)
		@ReferenceFieldTarget(label = "generic3")
		ArrayList<GenericTarget<?>> targetList = new ArrayList<>();

		@BitField(id = 5)
		@ReferenceField(stable = false, label = "generic3")
		ArrayList<GenericTarget<?>> referenceList = new ArrayList<>();

		@BitField(id = 6)
		@NestedFieldSetting(path = "k", fieldName = "TARGET")
		@NestedFieldSetting(path = "v", fieldName = "TARGET")
		HashMap<GenericTarget<?>, GenericTarget<?>> targetMap = new HashMap<>();

		@BitField(id = 7)
		@NestedFieldSetting(path = "k", fieldName = "REFERENCE")
		@NestedFieldSetting(path = "v", fieldName = "REFERENCE")
		HashMap<GenericTarget<?>, GenericTarget<?>> referenceMap = new HashMap<>();

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "generic4")
		private static final boolean TARGET = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "generic4")
		private static final boolean REFERENCE = false;
	}

	@Test
	public void testGenericTarget() {
		Bitser bitser = new Bitser();
		GenericRoot original = new GenericRoot();
		original.target = new GenericTarget<String>();
		original.reference = original.target;
		original.targets = new GenericTarget[] { new GenericTarget<Integer>() };
		original.references = new GenericTarget[] { original.targets[0] };
		original.targetList.add(new GenericTarget<Double>());
		original.referenceList.add(original.targetList.get(0));
		original.targetMap.put(new GenericTarget<Long>(), new GenericTarget<Class<?>>());
		original.referenceMap.put(
				original.targetMap.keySet().iterator().next(),
				original.targetMap.values().iterator().next()
		);

		GenericRoot loaded = bitser.stupidDeepCopy(original);
		assertSame(loaded.target, loaded.reference);
		assertSame(loaded.targets[0], loaded.references[0]);
		assertSame(loaded.targetList.get(0), loaded.referenceList.get(0));
		assertSame(loaded.targetMap.keySet().iterator().next(), loaded.referenceMap.keySet().iterator().next());
		assertSame(loaded.targetMap.values().iterator().next(), loaded.referenceMap.values().iterator().next());

		GenericRoot backward = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertSame(backward.target, backward.reference);
		assertSame(backward.targets[0], backward.references[0]);
		assertSame(backward.targetList.get(0), backward.referenceList.get(0));
		assertSame(backward.targetMap.keySet().iterator().next(), backward.referenceMap.keySet().iterator().next());
		assertSame(backward.targetMap.values().iterator().next(), backward.referenceMap.values().iterator().next());
		assertTrue(bitser.deepEquals(original, backward));
		assertEquals(bitser.hashCode(original), bitser.hashCode(backward));
	}

	@BitStruct(backwardCompatible = true)
	private static class FunctionReferenceTarget {

		private ItemType type1, type2;

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "item types")
		private ItemType typeA;

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "item types")
		private ItemType typeB;

		@BitField(id = 3)
		@ReferenceField(stable = false, label = "item types")
		private ItemType typeC;

		@BitField(id = 2)
		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "item types")
		ItemType[] getTargets() {
			return new ItemType[] { type1, type2 };
		}
	}

	@Test
	public void testFunctionReferenceTarget() {
		var original = new FunctionReferenceTarget();
		original.type1 = new ItemType("Sword");
		original.type2 = new ItemType("Shield");
		original.typeA = original.type2;
		original.typeB = original.type1;
		original.typeC = original.type2;

		var bitser = new Bitser();
		var simple = bitser.stupidDeepCopy(original);
		assertEquals("Shield", simple.typeA.name);
		assertEquals("Sword", simple.typeB.name);
		assertSame(simple.typeA, simple.typeC);

		var backward = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("Shield", backward.typeA.name);
		assertEquals("Sword", backward.typeB.name);
		assertSame(backward.typeA, backward.typeC);
	}

	@Test
	public void testFunctionReferenceTargetOnWithObject() {
		var with = new FunctionReferenceTarget();
		with.type1 = new ItemType("with1");
		with.type2 = new ItemType("with2");
		with.typeA = with.type1;
		with.typeB = with.type2;
		with.typeC = with.type2;

		var main = new FunctionReferenceTarget();
		main.type1 = new ItemType("unused1");
		main.type2 = new ItemType("unused2");
		main.typeA = with.typeA;
		main.typeB = with.typeB;
		main.typeC = with.typeC;

		var bitser = new Bitser();
		var basic = bitser.stupidDeepCopy(main, with);
		assertSame(with.typeA, basic.typeA);

		var backward = bitser.stupidDeepCopy(main, with, Bitser.BACKWARD_COMPATIBLE);
		assertSame(with.typeA, backward.typeA);
	}

	@BitStruct(backwardCompatible = true)
	private static class FunctionReference1 {

		@BitField(id = 2, readsMethodResult = true)
		private ItemType reference;

		@BitField(id = 5)
		@ReferenceFieldTarget(label = "item types")
		private ItemType typeA;

		@BitField(id = 6)
		@ReferenceFieldTarget(label = "item types")
		private ItemType typeB;

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "item types")
		ItemType getReference(FunctionContext context) {
			if (context.withParameters.containsKey("a")) return typeA;
			else return typeB;
		}
	}

	@Test
	public void testFunctionReference1() {
		var original = new FunctionReference1();
		original.typeA = new ItemType("Shield");
		original.typeB = new ItemType("Sword");

		var bitser = new Bitser();
		var simpleA = bitser.stupidDeepCopy(original, new WithParameter("a", null));
		assertEquals("Shield", simpleA.typeA.name);
		assertEquals("Sword", simpleA.typeB.name);
		assertSame(simpleA.typeA, simpleA.reference);

		var simpleB = bitser.stupidDeepCopy(original);
		assertEquals("Shield", simpleB.typeA.name);
		assertEquals("Sword", simpleB.typeB.name);
		assertSame(simpleB.typeB, simpleB.reference);

		var backwardA = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE, new WithParameter("a", null));
		assertEquals("Shield", backwardA.typeA.name);
		assertEquals("Sword", backwardA.typeB.name);
		assertSame(backwardA.typeA, backwardA.reference);

		var backwardB = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("Shield", backwardB.typeA.name);
		assertEquals("Sword", backwardB.typeB.name);
		assertSame(backwardB.typeB, backwardB.reference);
	}

	@BitStruct(backwardCompatible = true)
	private static class FunctionReference2 implements BitPostInit {

		private ItemType reference;

		@BitField(id = 5)
		@ReferenceFieldTarget(label = "item types")
		private ItemType typeA;

		@BitField(id = 6)
		@ReferenceFieldTarget(label = "item types")
		private ItemType typeB;

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "item types")
		ItemType getReference(FunctionContext context) {
			if (context.withParameters.containsKey("a")) return typeA;
			else return typeB;
		}

		@Override
		public void postInit(Context context) {
			this.reference = (ItemType) context.values.get(FunctionReference2.class)[2];
			assertNotNull(this.reference);
			assertTrue(this.reference == this.typeA || this.reference == this.typeB);
			if (context.backwardCompatible) {
				var legacy = context.legacyValues.get(FunctionReference2.class);
				var legacyReference = ((LegacyReference) legacy[2]).reference();
				assertNotNull(legacyReference);
				assertTrue(legacyReference == legacy[5] || legacyReference == legacy[6]);
			}
		}
	}

	@Test
	public void testFunctionReference2() {
		var original = new FunctionReference2();
		original.typeA = new ItemType("Shield");
		original.typeB = new ItemType("Sword");

		var bitser = new Bitser();
		var simpleA = bitser.stupidDeepCopy(original, new WithParameter("a", null));
		assertEquals("Shield", simpleA.typeA.name);
		assertEquals("Sword", simpleA.typeB.name);
		assertSame(simpleA.typeA, simpleA.reference);

		var simpleB = bitser.stupidDeepCopy(original);
		assertEquals("Shield", simpleB.typeA.name);
		assertEquals("Sword", simpleB.typeB.name);
		assertSame(simpleB.typeB, simpleB.reference);

		var backwardA = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE, new WithParameter("a", null));
		assertEquals("Shield", backwardA.typeA.name);
		assertEquals("Sword", backwardA.typeB.name);
		assertSame(backwardA.typeA, backwardA.reference);

		var backwardB = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("Shield", backwardB.typeA.name);
		assertEquals("Sword", backwardB.typeB.name);
		assertSame(backwardB.typeB, backwardB.reference);
	}

	@Test
	public void testDeepEqualsAndHashCode() {
		var bitser = new Bitser();
		var typeA1 = new ItemType("t1");
		var typeA2 = new ItemType("t2");
		var typeX = new ItemType("x");
		var typeY = new ItemType("y");

		var withRoot = new ItemRoot();
		withRoot.types.add(typeX);
		withRoot.types.add(typeY);

		var rootA = new ItemRoot();
		rootA.types.add(typeA1);
		rootA.types.add(typeA2);
		rootA.items.add(new Item("i1", typeA1));
		rootA.items.add(new Item("ix", typeX));

		var rootB = bitser.deepCopy(rootA);
		assertTrue(bitser.deepEquals(rootA, rootB));
		assertEquals(bitser.hashCode(rootA), bitser.hashCode(rootB));
		assertNotSame(rootA.items.get(0).type, rootB.items.get(0).type);
		assertSame(rootA.items.get(1).type, rootB.items.get(1).type);

		rootB.items.set(1, new Item("ix", typeY));
		assertFalse(bitser.deepEquals(rootA, rootB));

		rootB.items.set(1, new Item("ix", typeX));
		assertTrue(bitser.deepEquals(rootA, rootB));

		rootB.items.set(0, new Item("i1", rootB.types.get(1)));
		assertFalse(bitser.deepEquals(rootA, rootB));

		rootB.items.set(0, new Item("i1", rootA.types.get(0)));
		assertFalse(bitser.deepEquals(rootA, rootB));

		rootB.items.set(0, new Item("i1", rootB.types.get(0)));
		assertTrue(bitser.deepEquals(rootA, rootB));
	}
}
