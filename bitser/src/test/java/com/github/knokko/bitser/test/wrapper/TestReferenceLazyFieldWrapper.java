package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.ReferenceLazyBits;
import com.github.knokko.bitser.SimpleLazyBits;
import com.github.knokko.bitser.distributions.FloatDistributionTracker;
import com.github.knokko.bitser.distributions.IntegerDistributionTracker;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.options.WithParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestReferenceLazyFieldWrapper {

	@BitStruct(backwardCompatible = true)
	private static class SimpleInnerStruct {

		@BitField(id = 0)
		final ArrayList<String> words = new ArrayList<>();

		@BitField(id = 1)
		@StableReferenceFieldId
		final UUID id = UUID.randomUUID();
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferencingInnerStruct {

		@BitField(id = 0)
		@ReferenceField(stable = false, label = "unstable")
		final ArrayList<SimpleInnerStruct> unstable = new ArrayList<>();

		@BitField(id = 1)
		@ReferenceField(stable = true, label = "stable")
		final ArrayList<SimpleInnerStruct> stable = new ArrayList<>();

		@BitField(id = 2, optional = true)
		@ReferenceField(stable = false, label = "unstable")
		SimpleInnerStruct optionalUnstable;

		@BitField(id = 3, optional = true)
		@ReferenceField(stable = true, label = "stable")
		SimpleInnerStruct optionalStable;
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleOuterStruct {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int prefix;

		@BitField(id = 1)
		@LazyReferences(labels = { "stable", "extra" })
		ReferenceLazyBits<SimpleInnerStruct> lazy;

		@BitField(id = 2)
		@IntegerField(expectUniform = false)
		int suffix;
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferencingOuterStruct {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int prefix;

		@BitField(id = 1)
		@LazyReferences(labels = { "stable", "unstable", "extra" })
		ReferenceLazyBits<ReferencingInnerStruct> lazy;

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "unstable")
		final ArrayList<SimpleInnerStruct> unstableTargets = new ArrayList<>();

		@BitField(id = 3)
		@ReferenceFieldTarget(label = "stable")
		final ArrayList<SimpleInnerStruct> stableTargets = new ArrayList<>();

		@BitField(id = 4)
		@IntegerField(expectUniform = false)
		int suffix;
	}

	@Test
	public void testWithoutReferences() {
		Bitser bitser = new Bitser();
		var input = new SimpleOuterStruct();
		input.prefix = 123;
		input.lazy = new ReferenceLazyBits<>(new SimpleInnerStruct());
		input.suffix = 456;

		input.lazy.get().words.add("simple");
		input.lazy.get().words.add("test");

		var incompatible = bitser.stupidDeepCopy(input);
		assertEquals(123, incompatible.prefix);
		assertEquals(456, incompatible.suffix);
		assertEquals("simple", incompatible.lazy.get().words.get(0));
		assertEquals("test", incompatible.lazy.get().words.get(1));
		assertTrue(bitser.deepEquals(input, incompatible));

		var compatible = bitser.stupidDeepCopy(input, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(123, compatible.prefix);
		assertEquals(456, compatible.suffix);
		assertEquals("simple", compatible.lazy.get().words.get(0));
		assertEquals("test", compatible.lazy.get().words.get(1));
		assertEquals(bitser.hashCode(input), bitser.hashCode(compatible));
		assertTrue(bitser.deepEquals(input, compatible));
		compatible.lazy.get().words.add("no longer equal");
		assertNotEquals(bitser.hashCode(input), bitser.hashCode(compatible));
		assertFalse(bitser.deepEquals(input, compatible));
	}

	@Test
	public void testWithSimpleReferences() {
		Bitser bitser = new Bitser();
		var input = new ReferencingOuterStruct();
		input.prefix = 123;
		input.stableTargets.add(new SimpleInnerStruct());
		input.stableTargets.add(new SimpleInnerStruct());
		input.unstableTargets.add(new SimpleInnerStruct());
		input.unstableTargets.add(new SimpleInnerStruct());
		input.lazy = new ReferenceLazyBits<>(new ReferencingInnerStruct());
		input.lazy.get().stable.add(input.stableTargets.get(0));
		input.lazy.get().unstable.add(input.unstableTargets.get(1));
		input.suffix = 456;

		var incompatible = bitser.stupidDeepCopy(input);
		assertEquals(123, incompatible.prefix);
		assertEquals(456, incompatible.suffix);
		assertEquals(2, incompatible.stableTargets.size());
		assertEquals(2, incompatible.unstableTargets.size());
		assertSame(incompatible.stableTargets.get(0), incompatible.lazy.get().stable.get(0));
		assertSame(incompatible.unstableTargets.get(1), incompatible.lazy.get().unstable.get(0));
		assertTrue(bitser.deepEquals(input, incompatible));

		var compatible = bitser.stupidDeepCopy(input, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(123, compatible.prefix);
		assertEquals(456, compatible.suffix);
		assertEquals(2, compatible.stableTargets.size());
		assertEquals(2, compatible.unstableTargets.size());
		assertSame(compatible.stableTargets.get(0), compatible.lazy.get().stable.get(0));
		assertSame(compatible.unstableTargets.get(1), compatible.lazy.get().unstable.get(0));
		assertTrue(bitser.deepEquals(input, compatible));
	}

	@BitStruct(backwardCompatible = true)
	private static class NestedStruct {

		@BitField(id = 0, optional = true)
		@LazyReferences(labels = { "nested", "ids" })
		ReferenceLazyBits<NestedStruct> next;

		@BitField(id = 1, optional = true)
		@ReferenceField(stable = false, label = "nested")
		SimpleInnerStruct unstableReference;

		@BitField(id = 2, optional = true)
		@ReferenceField(stable = true, label = "nested")
		SimpleInnerStruct stableReference;

		@BitField(id = 3)
		@StableReferenceFieldId
		@ReferenceFieldTarget(label = "ids")
		final UUID id = UUID.randomUUID();

		@BitField(id = 4, optional = true)
		@ReferenceField(stable = false, label = "ids")
		UUID otherID;

		@BitField(id = 5)
		@ReferenceFieldTarget(label = "nested")
		final SimpleInnerStruct target = new SimpleInnerStruct();
	}

	@BitStruct(backwardCompatible = true)
	private static class WithStruct {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "ids")
		final UUID id = UUID.randomUUID();

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "nested")
		final SimpleInnerStruct target = new SimpleInnerStruct();
	}

	@Test
	public void testComplexNested() {
		var bitser = new Bitser();

		WithStruct[] with = { new WithStruct(), new WithStruct() };

		var root = new NestedStruct();
		root.otherID = root.id;
		root.stableReference = with[0].target;

		var child0 = new NestedStruct();
		root.next = new ReferenceLazyBits<>(child0);
		child0.otherID = root.id;
		child0.unstableReference = with[1].target;

		var child1 = new NestedStruct();
		child0.next = new ReferenceLazyBits<>(child1);
		child1.unstableReference = root.target;
		child1.stableReference = child0.target;
		child1.otherID = child0.id;

		var child2 = new NestedStruct();
		child1.next = new ReferenceLazyBits<>(child2);
		child2.unstableReference = child0.target;
		child2.stableReference = child1.target;
		child2.otherID = child0.id;

		var child3 = new NestedStruct();
		child2.next = new ReferenceLazyBits<>(child3);
		child3.unstableReference = with[1].target;
		child3.stableReference = with[0].target;
		child3.otherID = child2.id;

		NestedStruct[] original = { root, child0, child1, child2, child3 };
		checkComplexNested(bitser.stupidDeepCopy(root, with[0], with[1]), original, with);
		checkComplexNested(bitser.stupidDeepCopy(root, with[0], Bitser.BACKWARD_COMPATIBLE, with[1]), original, with);
		checkComplexNested(bitser.deepCopy(root), original, with);
	}

	private void checkComplexNested(NestedStruct root, NestedStruct[] original, WithStruct[] with) {
		var bitser = new Bitser();
		assertNotSame(original[0], root);
		assertSame(root.otherID, root.id);
		assertNull(root.unstableReference);
		assertSame(with[0].target, root.stableReference);
		assertTrue(bitser.deepEquals(original[0], root));
		assertEquals(bitser.hashCode(original[0]), bitser.hashCode(root));

		var child0 = root.next.get();
		assertSame(child0.otherID, root.id);
		assertSame(with[1].target, child0.unstableReference);
		assertNull(child0.stableReference);
		assertFalse(bitser.deepEquals(original[1], child0));
		assertEquals(bitser.hashCode(original[1]), bitser.hashCode(child0));

		var child1 = child0.next.get();
		assertNotSame(original[2], child1);
		assertSame(root.target, child1.unstableReference);
		assertSame(child0.target, child1.stableReference);
		assertSame(child0.id, child1.otherID);
		assertFalse(bitser.deepEquals(original[2], child1));
		assertEquals(bitser.hashCode(original[2]), bitser.hashCode(child1));

		var child2 = child1.next.get();
		assertNotSame(original[3], child2);
		assertSame(child0.target, child2.unstableReference);
		assertSame(child1.target, child2.stableReference);
		assertSame(child0.id, child2.otherID);
		assertFalse(bitser.deepEquals(original[3], child2));
		assertEquals(bitser.hashCode(original[3]), bitser.hashCode(child2));

		var child3 = child2.next.get();
		assertNotSame(original[4], child3);
		assertSame(with[1].target, child3.unstableReference);
		assertSame(with[0].target, child3.stableReference);
		assertSame(child2.id, child3.otherID);
		assertEquals(bitser.hashCode(original[4]), bitser.hashCode(child3));

		// Sanity check
		assertTrue(bitser.deepEquals(original[0], root));
		child3.otherID = child3.id;
		assertFalse(bitser.deepEquals(original[0], root));
		assertEquals(bitser.hashCode(original[0]), bitser.hashCode(root));
		child3.otherID = child2.id;
		child3.stableReference = null;
		assertNotEquals(bitser.hashCode(original[0]), bitser.hashCode(root));
		assertFalse(bitser.deepEquals(original[0], root));
	}

	@BitStruct(backwardCompatible = true)
	private static class RainbowReferences {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "int array")
		private static final boolean INT_ARRAY_REFERENCE = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "string array")
		private static final boolean STRING_ARRAY_REFERENCE = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "struct array")
		private static final boolean STRUCT_ARRAY_REFERENCE = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "uuid element")
		private static final boolean UUID_ELEMENT_REFERENCE = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = true, label = "struct element")
		private static final boolean STRUCT_ELEMENT_REFERENCE = false;

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "map")
		private static final boolean MAP_REFERENCE = false;

		@BitField(id = 0)
		@NestedFieldSetting(path = "", fieldName = "INT_ARRAY_REFERENCE")
		int[] intArray;

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "string element")
		String[] stringArray1;

		@BitField(id = 2)
		@NestedFieldSetting(path = "", fieldName = "STRING_ARRAY_REFERENCE")
		String[] stringArray2;

		@BitField(id = 3)
		@ReferenceField(stable = true, label = "struct element")
		SimpleInnerStruct[] structArray1;

		@BitField(id = 4)
		@NestedFieldSetting(path = "", fieldName = "STRUCT_ARRAY_REFERENCE")
		SimpleInnerStruct[] structArray2;

		@BitField(id = 5)
		@ReferenceField(stable = false, label = "string element")
		final ArrayList<String> stringList1 = new ArrayList<>();

		@BitField(id = 6)
		@NestedFieldSetting(path = "", fieldName = "STRING_ARRAY_REFERENCE")
		ArrayList<String> stringList2;

		@BitField(id = 7)
		@NestedFieldSetting(path = "k", fieldName = "UUID_ELEMENT_REFERENCE")
		@NestedFieldSetting(path = "v", fieldName = "STRUCT_ELEMENT_REFERENCE")
		final HashMap<UUID, SimpleInnerStruct> map1 = new HashMap<>();

		@BitField(id = 8)
		@NestedFieldSetting(path = "", fieldName = "MAP_REFERENCE")
		HashMap<UUID, SimpleInnerStruct> map2;
	}

	@BitStruct(backwardCompatible = true)
	private static class RainbowTargets {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "int array")
		private static final boolean INT_ARRAY_TARGET = false;

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "string array")
		private static final boolean STRING_ARRAY_TARGET = false;

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "struct array")
		private static final boolean STRUCT_ARRAY_TARGET = false;

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "map")
		private static final boolean MAP_TARGET = false;

		@BitField(id = 0)
		@LazyReferences(labels = {
				"string element", "uuid element", "struct element",
				"int array", "string array", "struct array", "map"
		})
		final ReferenceLazyBits<RainbowReferences> lazy = new ReferenceLazyBits<>(new RainbowReferences());

		@BitField(id = 1)
		@ReferenceFieldTarget(label = "string element")
		String stringElement = "abc";

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "uuid element")
		UUID uuidElement = new UUID(1, 2);

		@BitField(id = 3)
		@ReferenceFieldTarget(label = "struct element")
		SimpleInnerStruct structElement = new SimpleInnerStruct();

		@BitField(id = 4)
		@IntegerField(expectUniform = false)
		@NestedFieldSetting(path = "", fieldName = "INT_ARRAY_TARGET")
		final int[] intArray = { 4, 1, 6, 9 };

		@BitField(id = 5)
		@NestedFieldSetting(path = "", fieldName = "STRING_ARRAY_TARGET")
		final String[] stringArray = { "ui", "bits" };

		@BitField(id = 6)
		@NestedFieldSetting(path = "", fieldName = "STRUCT_ARRAY_TARGET")
		final SimpleInnerStruct[] structArray = { new SimpleInnerStruct() };

		@BitField(id = 7)
		@NestedFieldSetting(path = "", fieldName = "MAP_TARGET")
		final HashMap<UUID, SimpleInnerStruct> map = new HashMap<>();

		@BitField(id = 8)
		@NestedFieldSetting(path = "", fieldName = "STRING_ARRAY_TARGET")
		final ArrayList<String> stringList = new ArrayList<>();
	}

	@Test
	public void testAllTypesOfReferences() {
		var bitser = new Bitser();

		var original = new RainbowTargets();
		original.lazy.get().intArray = original.intArray;
		original.lazy.get().stringArray1 = new String[] { original.stringElement };
		original.lazy.get().stringArray2 = original.stringArray;
		original.lazy.get().structArray1 = new SimpleInnerStruct[] { original.structElement };
		original.lazy.get().structArray2 = original.structArray;
		original.lazy.get().stringList1.add(original.stringElement);
		original.lazy.get().stringList2 = original.stringList;
		original.lazy.get().map1.put(original.uuidElement, original.structElement);
		original.lazy.get().map2 = original.map;

		checkAllTypesOfReferences(original, bitser.stupidDeepCopy(original));
		checkAllTypesOfReferences(original, bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE));
		checkAllTypesOfReferences(original, bitser.deepCopy(original));
	}

	private void checkAllTypesOfReferences(RainbowTargets original, RainbowTargets copied) {
		assertNotSame(original.lazy.get(), copied.lazy.get());
		assertSame(copied.intArray, copied.lazy.get().intArray);

		assertNotSame(copied.stringArray, copied.lazy.get().stringArray1);
		assertSame(copied.stringElement, copied.lazy.get().stringArray1[0]);
		assertSame(copied.stringArray, copied.lazy.get().stringArray2);

		assertNotSame(copied.structArray, copied.lazy.get().structArray1);
		assertSame(copied.structElement, copied.lazy.get().structArray1[0]);
		assertSame(copied.structArray, copied.lazy.get().structArray2);

		assertNotSame(copied.stringList, copied.lazy.get().stringList1);
		assertSame(copied.stringElement, copied.lazy.get().stringList1.get(0));
		assertSame(copied.stringList, copied.lazy.get().stringList2);

		assertSame(copied.uuidElement, copied.lazy.get().map1.keySet().iterator().next());
		assertSame(copied.structElement, copied.lazy.get().map1.values().iterator().next());
		assertSame(copied.map, copied.lazy.get().map2);
	}

	@BitStruct(backwardCompatible = true)
	private static class ConversionReferences {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "unstable")
		final SimpleInnerStruct unstableTarget = new SimpleInnerStruct();

		@BitField(id = 1)
		@LazyReferences(labels = { "unstable", "stable" })
		ReferenceLazyBits<ReferencingInnerStruct> lazy;

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "stable")
		final SimpleInnerStruct stableTarget = new SimpleInnerStruct();
	}

	@BitStruct(backwardCompatible = true)
	private static class ConversionSimple {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "unstable")
		final SimpleInnerStruct unstableTarget = new SimpleInnerStruct();

		@BitField(id = 1)
		SimpleLazyBits<ReferencingInnerStruct> lazy;

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "stable")
		final SimpleInnerStruct stableTarget = new SimpleInnerStruct();
	}

	@BitStruct(backwardCompatible = true)
	private static class ConversionEager {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "unstable")
		final SimpleInnerStruct unstableTarget = new SimpleInnerStruct();

		@BitField(id = 1)
		ReferencingInnerStruct inner;

		@BitField(id = 2)
		@ReferenceFieldTarget(label = "stable")
		final SimpleInnerStruct stableTarget = new SimpleInnerStruct();
	}

	@Test
	public void testConvertBetweenEagerAndLazyReferences() {
		var bitser = new Bitser();

		var original = new ConversionEager();
		original.inner = new ReferencingInnerStruct();
		original.inner.unstable.add(original.unstableTarget);
		original.inner.stable.add(original.stableTarget);
		original.inner.optionalUnstable = original.unstableTarget;
		original.inner.optionalStable = original.stableTarget;

		var lazyReferences = bitser.fromBytes(
				ConversionReferences.class,
				bitser.toBytes(original, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertSame(lazyReferences.unstableTarget, lazyReferences.lazy.get().unstable.get(0));
		assertSame(lazyReferences.stableTarget, lazyReferences.lazy.get().stable.get(0));
		assertSame(lazyReferences.unstableTarget, lazyReferences.lazy.get().optionalUnstable);
		assertSame(lazyReferences.stableTarget, lazyReferences.lazy.get().optionalStable);

		var eager = bitser.fromBytes(
				ConversionEager.class,
				bitser.toBytes(lazyReferences, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertSame(eager.unstableTarget, eager.inner.unstable.get(0));
		assertSame(eager.stableTarget, eager.inner.stable.get(0));
		assertSame(eager.unstableTarget, eager.inner.optionalUnstable);
		assertSame(eager.stableTarget, eager.inner.optionalStable);
	}

	@Test
	public void testConvertBetweenLazySimpleAndLazyReferences() {
		var bitser = new Bitser();

		var simple1 = new ConversionSimple();
		simple1.lazy = new SimpleLazyBits<>(new ReferencingInnerStruct());

		var referencing1 = bitser.fromBytes(
				ConversionReferences.class,
				bitser.toBytes(simple1, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(0, referencing1.lazy.get().unstable.size());
		assertEquals(0, referencing1.lazy.get().stable.size());

		referencing1.lazy.get().unstable.add(referencing1.unstableTarget);
		referencing1.lazy.get().stable.add(referencing1.stableTarget);
		referencing1.lazy.get().optionalUnstable = referencing1.unstableTarget;
		referencing1.lazy.get().optionalStable = referencing1.stableTarget;

		var simple2 = bitser.fromBytes(
				ConversionSimple.class,
				bitser.toBytes(referencing1, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertSame(simple2.unstableTarget, simple2.lazy.get().unstable.get(0));
		assertSame(simple2.stableTarget, simple2.lazy.get().stable.get(0));
		assertSame(simple2.unstableTarget, simple2.lazy.get().optionalUnstable);
		assertSame(simple2.stableTarget, simple2.lazy.get().optionalStable);

		String errorMessage = assertThrows(ReferenceBitserException.class, () -> bitser.toBytes(simple2)).getMessage();
		assertContains(errorMessage, "ConversionSimple -> lazy");
		assertContains(errorMessage, "ReferencingInnerStruct -> optionalStable");
		assertContains(errorMessage, "Can't find @ReferenceFieldTarget with label stable");
	}

	@BitStruct(backwardCompatible = true)
	private static class UsesWithOptions {

		@BitField(id = 0)
		@FloatField
		float factor;

		@IntegerField(expectUniform = false)
		@BitField(id = 1)
		int intValue;

		@BitField(id = 2, readsMethodResult = true)
		float product;

		@FloatField
		@BitField(id = 2)
		float shouldMultiply(FunctionContext context) {
			return factor * (Float) context.withParameters.get("right");
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class WithOptionsParent {

		@BitField(id = 0)
		SimpleLazyBits<UsesWithOptions> simpleLazy;

		@BitField(id = 1)
		@LazyReferences(labels = {})
		ReferenceLazyBits<UsesWithOptions> referenceLazy;
	}

	@Test
	public void testWithOptions() {
		var bitser = new Bitser();
		var original = new WithOptionsParent();
		original.simpleLazy = new SimpleLazyBits<>(new UsesWithOptions());
		original.simpleLazy.get().factor = 2f;
		original.referenceLazy = new ReferenceLazyBits<>(new UsesWithOptions());
		original.referenceLazy.get().factor = 3f;

		var ints = new IntegerDistributionTracker();
		var floats = new FloatDistributionTracker();
		var simple = bitser.stupidDeepCopy(
				original, new WithParameter("right", 5f), ints, floats, Bitser.FORBID_LAZY_SAVING
		);
		checkWithOptions(simple, ints, floats);

		ints = new IntegerDistributionTracker();
		floats = new FloatDistributionTracker();
		var backward = bitser.stupidDeepCopy(
				original, new WithParameter("right", 5f), ints, floats, Bitser.FORBID_LAZY_SAVING
		);
		checkWithOptions(backward, ints, floats);
	}

	private void checkWithOptions(
			WithOptionsParent parent,
			IntegerDistributionTracker ints,
			FloatDistributionTracker floats
	) {
		assertEquals(2f, parent.simpleLazy.get().factor);
		assertEquals(10f, parent.simpleLazy.get().product);
		assertEquals(3f, parent.referenceLazy.get().factor);
		assertEquals(15f, parent.referenceLazy.get().product);

		assertEquals(1, ints.mapping.size());
		var intValues = ints.mapping.values().iterator().next();
		assertEquals(1, intValues.countMap.size());
		assertEquals(2, intValues.countMap.get(0L));

		assertEquals(2, floats.mapping.size());
		floats.mapping.forEach((field, floatValues) -> {
			assertEquals(2, floatValues.countMap.size());
			if (field.contains("shouldMultiply")) {
				assertEquals(1, floatValues.countMap.get(10.0));
				assertEquals(1, floatValues.countMap.get(15.0));
			} else {
				assertEquals(1, floatValues.countMap.get(2.0));
				assertEquals(1, floatValues.countMap.get(3.0));
			}
		});
	}

	@BitStruct(backwardCompatible = true)
	private static class LazyCollections {

		@SuppressWarnings("unused")
		@LazyReferences(labels = { "stable", "unstable" })
		private static final boolean MAP_VALUE_PROPERTIES = false;

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "stable")
		SimpleInnerStruct stableTarget;

		@BitField(id = 1)
		@LazyReferences(labels = { "stable", "unstable" })
		ReferenceLazyBits<ReferencingInnerStruct>[] array;

		@BitField(id = 2)
		@LazyReferences(labels = { "stable", "unstable" })
		final ArrayList<ReferenceLazyBits<ReferencingInnerStruct>> list = new ArrayList<>();

		@BitField(id = 3)
		@NestedFieldSetting(path = "v", fieldName = "MAP_VALUE_PROPERTIES")
		final HashMap<String, ReferenceLazyBits<ReferencingInnerStruct>> map = new HashMap<>();

		@BitField(id = 4)
		@ReferenceFieldTarget(label = "unstable")
		SimpleInnerStruct[] unstableTargets;
	}

	@Test
	public void testLazyCollections() {
		var original = new LazyCollections();
		original.stableTarget = new SimpleInnerStruct();
		original.unstableTargets = new SimpleInnerStruct[] { new SimpleInnerStruct(), new SimpleInnerStruct() };

		//noinspection unchecked
		original.array = new ReferenceLazyBits[] {
				new ReferenceLazyBits<>(new ReferencingInnerStruct())
		};
		original.array[0].get().optionalStable = original.stableTarget;
		original.array[0].get().optionalUnstable = original.unstableTargets[0];

		original.list.add(new ReferenceLazyBits<>(new ReferencingInnerStruct()));
		original.list.get(0).get().optionalStable = original.stableTarget;
		original.list.get(0).get().optionalUnstable = original.unstableTargets[1];

		var mapValue = new ReferenceLazyBits<>(new ReferencingInnerStruct());
		mapValue.get().stable.add(original.stableTarget);
		mapValue.get().unstable.add(original.unstableTargets[1]);
		original.map.put("singleton", mapValue);

		var bitser = new Bitser();
		checkLazyCollections(bitser.stupidDeepCopy(original));
		checkLazyCollections(bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE));
	}

	private void checkLazyCollections(LazyCollections result) {
		assertEquals(2, result.unstableTargets.length);
		assertNotSame(result.unstableTargets[0], result.unstableTargets[1]);

		assertEquals(1, result.array.length);
		assertSame(result.stableTarget, result.array[0].get().optionalStable);
		assertSame(result.unstableTargets[0], result.array[0].get().optionalUnstable);

		assertEquals(1, result.list.size());
		assertSame(result.stableTarget, result.list.get(0).get().optionalStable);
		assertSame(result.unstableTargets[1], result.list.get(0).get().optionalUnstable);

		assertEquals(1, result.map.size());
		var mapValue = result.map.get("singleton");
		assertSame(result.stableTarget, mapValue.get().stable.get(0));
		assertSame(result.unstableTargets[1], mapValue.get().unstable.get(0));
	}

	@BitStruct(backwardCompatible = false)
	private static class MissingAnnotation {

		@SuppressWarnings("unused")
		ReferenceLazyBits<SimpleInnerStruct> invalidLazy;
	}

	@Test
	public void testMissingAnnotationError() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new MissingAnnotation())
		).getMessage();
		assertContains(errorMessage, "TestReferenceLazyFieldWrapper$MissingAnnotation.invalidLazy");
		assertContains(errorMessage, "must be annotated with @LazyReferences");
	}

	@BitStruct(backwardCompatible = false)
	private static class AnnotationOnWrongType {

		@SuppressWarnings("unused")
		@LazyReferences(labels = { "test" })
		String invalidLazy;
	}

	@Test
	public void testWrongTypeError() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new AnnotationOnWrongType())
		).getMessage();
		assertContains(errorMessage, "@LazyReferences annotation is only allowed on ReferenceLazyBits");
		assertContains(errorMessage, "TestReferenceLazyFieldWrapper$AnnotationOnWrongType.invalidLazy");
	}
}
