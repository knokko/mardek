package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.SimpleLazyBits;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.exceptions.ReferenceBitserException;
import com.github.knokko.bitser.field.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestSimpleLazyFieldWrapper {

	@BitStruct(backwardCompatible = true)
	private static class SimpleInnerStruct {

		@SuppressWarnings("unused")
		private static final Class<?>[] BITSER_HIERARCHY = { SimpleInnerStruct.class };

		@BitField(id = 0)
		final ArrayList<String> words = new ArrayList<>();
	}

	@BitStruct(backwardCompatible = true)
	private static class DifferentInnerStruct {

		@BitField(id = 0)
		@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
		final ArrayList<String> words = new ArrayList<>();

		@BitField(id = 1)
		String separator = " ";
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleOuterStruct {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		int prefix;

		@BitField(id = 1)
		SimpleLazyBits<SimpleInnerStruct> lazy;

		@BitField(id = 2)
		@IntegerField(expectUniform = false)
		int suffix;
	}

	@BitStruct(backwardCompatible = true)
	private static class DifferentOuterStruct {

		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		long prefix;

		@BitField(id = 1)
		SimpleLazyBits<DifferentInnerStruct> lazy;

		@BitField(id = 2)
		@IntegerField(expectUniform = false)
		int suffix;
	}

	@Test
	public void testSimple() {
		Bitser bitser = new Bitser();
		SimpleOuterStruct input = new SimpleOuterStruct();
		input.prefix = 123;
		input.lazy = new SimpleLazyBits<>(new SimpleInnerStruct());
		input.suffix = 456;

		input.lazy.get().words.add("simple");
		input.lazy.get().words.add("test");

		SimpleOuterStruct incompatible = bitser.stupidDeepCopy(input);
		assertEquals(123, incompatible.prefix);
		assertEquals(456, incompatible.suffix);
		assertEquals("simple", incompatible.lazy.get().words.get(0));
		assertEquals("test", incompatible.lazy.get().words.get(1));
		assertTrue(bitser.deepEquals(input, incompatible));

		SimpleOuterStruct compatible = bitser.stupidDeepCopy(input, Bitser.BACKWARD_COMPATIBLE);
		DifferentOuterStruct different2 = bitser.fromBytes(
				DifferentOuterStruct.class,
				bitser.toBytes(input, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(123, compatible.prefix);
		assertEquals(456, compatible.suffix);
		assertEquals("simple", compatible.lazy.get().words.get(0));
		assertEquals("test", compatible.lazy.get().words.get(1));
		assertEquals(bitser.hashCode(input), bitser.hashCode(compatible));
		assertTrue(bitser.deepEquals(input, compatible));
		compatible.lazy.get().words.add("no longer equal");
		assertNotEquals(bitser.hashCode(input), bitser.hashCode(compatible));
		assertFalse(bitser.deepEquals(input, compatible));

		DifferentOuterStruct different1 = bitser.fromBytes(
				DifferentOuterStruct.class,
				bitser.toBytes(input, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);
		assertEquals(123L, different1.prefix);
		assertEquals(456, different1.suffix);
		assertEquals("simple", different1.lazy.get().words.get(0));
		assertEquals("test", different1.lazy.get().words.get(1));
		assertEquals(" ", different1.lazy.get().separator);

		assertEquals(123L, different2.prefix);
		assertEquals(456, different2.suffix);
		assertEquals("simple", different2.lazy.get().words.get(0));
		assertEquals("test", different2.lazy.get().words.get(1));
		assertEquals(" ", different2.lazy.get().separator);
	}

	@Test
	public void testNull() {
		String errorMessage = assertThrows(
				InvalidBitValueException.class,
				() -> new Bitser().toBytes(new SimpleOuterStruct())
		).getMessage();
		assertContains(errorMessage, "-> lazy");
		assertContains(errorMessage, "must not be null");
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalOuterStruct {
		@BitField(id = 0, optional = true)
		SimpleLazyBits<SimpleInnerStruct> lazy;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int suffix;
	}

	@Test
	public void testOptional() {
		var bitser = new Bitser();
		OptionalOuterStruct outer = new OptionalOuterStruct();
		outer.suffix = 12;

		OptionalOuterStruct incompatible = bitser.stupidDeepCopy(outer);
		assertNull(incompatible.lazy);
		assertEquals(12, incompatible.suffix);

		OptionalOuterStruct compatible = bitser.stupidDeepCopy(outer, Bitser.BACKWARD_COMPATIBLE);
		assertNull(compatible.lazy);
		assertEquals(12, compatible.suffix);
		assertEquals(bitser.hashCode(outer), bitser.hashCode(compatible));
		assertEquals(bitser.hashCode(outer), bitser.hashCode(incompatible));
		assertTrue(bitser.deepEquals(outer, compatible));

		outer.lazy = new SimpleLazyBits<>(new SimpleInnerStruct());
		assertNotEquals(bitser.hashCode(outer), bitser.hashCode(compatible));
		assertFalse(bitser.deepEquals(outer, compatible));

		incompatible = bitser.stupidDeepCopy(outer);
		assertNotNull(incompatible.lazy.get());
		assertEquals(12, incompatible.suffix);

		compatible = bitser.stupidDeepCopy(outer, Bitser.BACKWARD_COMPATIBLE);
		assertNotNull(compatible.lazy.get());
		assertEquals(12, compatible.suffix);
	}

	@BitStruct(backwardCompatible = true)
	private static class NotLazy {

		@BitField(id = 1)
		SimpleInnerStruct inner;
	}

	@Test
	public void testLazyToNonLazy() {
		SimpleOuterStruct input = new SimpleOuterStruct();
		input.lazy = new SimpleLazyBits<>(new SimpleInnerStruct());
		input.lazy.get().words.add("not anymore");

		Bitser bitser = new Bitser();

		NotLazy output = bitser.fromBytes(
				NotLazy.class,
				bitser.toBytes(input, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);

		assertEquals("not anymore", output.inner.words.get(0));
	}

	@Test
	public void testNonLazyToLazy() {
		NotLazy input = new NotLazy();
		input.inner = new SimpleInnerStruct();
		input.inner.words.add("become lazy");

		Bitser bitser = new Bitser();

		SimpleOuterStruct output = bitser.fromBytes(
				SimpleOuterStruct.class,
				bitser.toBytes(input, Bitser.BACKWARD_COMPATIBLE),
				Bitser.BACKWARD_COMPATIBLE
		);

		assertEquals("become lazy", output.lazy.get().words.get(0));
	}

	@BitStruct(backwardCompatible = true)
	private static class StringStruct0 {

		@BitField(id = 0, optional = true)
		String test;

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		int suffix;
	}

	@BitStruct(backwardCompatible = true)
	private static class StringStruct1 {

		@BitField(id = 1)
		@SuppressWarnings("unused")
		String test;
	}

	@Test
	public void testLazyToNonLazyString() {
		SimpleOuterStruct input = new SimpleOuterStruct();
		input.lazy = new SimpleLazyBits<>(new SimpleInnerStruct());
		input.lazy.get().words.add("not anymore");

		Bitser bitser = new Bitser();
		byte[] bytes = bitser.toBytes(input, Bitser.BACKWARD_COMPATIBLE);

		String errorMessage = assertThrows(
				LegacyBitserException.class,
				() -> bitser.fromBytes(StringStruct1.class, bytes, Bitser.BACKWARD_COMPATIBLE)
		).getMessage();
		assertContains(errorMessage, "LegacyLazyBytes");
		assertContains(errorMessage, "java.lang.String");
		assertContains(errorMessage, "TestSimpleLazyFieldWrapper$StringStruct1.test");
	}

	@Test
	public void testLazyNullToNonLazyNullString() {
		Bitser bitser = new Bitser();
		OptionalOuterStruct optional = new OptionalOuterStruct();
		optional.suffix = 456;
		byte[] bytes = bitser.toBytes(optional, Bitser.BACKWARD_COMPATIBLE);
		StringStruct0 stringStruct = bitser.fromBytes(
				StringStruct0.class, bytes, Bitser.BACKWARD_COMPATIBLE
		);
		assertNull(stringStruct.test);
		assertEquals(456, stringStruct.suffix);
	}

	@BitStruct(backwardCompatible = true)
	private static class NonLazyOptional {

		@BitField(id = 0, optional = true)
		SimpleInnerStruct inner;

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		byte suffix;
	}

	@Test
	public void testLazyNullToNonLazyNullStruct() {
		Bitser bitser = new Bitser();
		OptionalOuterStruct optional = new OptionalOuterStruct();
		optional.suffix = 123;
		byte[] bytes = bitser.toBytes(optional, Bitser.BACKWARD_COMPATIBLE);
		NonLazyOptional nonLazy = bitser.fromBytes(
				NonLazyOptional.class, bytes, Bitser.BACKWARD_COMPATIBLE
		);
		assertNull(nonLazy.inner);
		assertEquals((byte) 123, nonLazy.suffix);
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceInnerStruct {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "example")
		String target = "target" + Math.random();

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "example")
		String reference;
	}

	@BitStruct(backwardCompatible = true)
	private static class ReferenceOuterStruct {

		@BitField(id = 0)
		@ReferenceFieldTarget(label = "example")
		String target = "target" + Math.random();

		@BitField(id = 1)
		SimpleLazyBits<ReferenceInnerStruct> lazy;

		@BitField(id = 2)
		@ReferenceField(stable = false, label = "example")
		String reference;
	}

	@Test
	public void testAllowOwnReferences() {
		Bitser bitser = new Bitser();
		ReferenceOuterStruct original = new ReferenceOuterStruct();
		original.reference = original.target;
		original.lazy = new SimpleLazyBits<>(new ReferenceInnerStruct());
		original.lazy.get().reference = original.lazy.get().target;

		ReferenceOuterStruct incompatible = bitser.stupidDeepCopy(original);
		assertSame(incompatible.target, incompatible.reference);
		assertSame(incompatible.lazy.get().target, incompatible.lazy.get().reference);

		ReferenceOuterStruct compatible = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertSame(compatible.target, compatible.reference);
		assertSame(compatible.lazy.get().target, compatible.lazy.get().reference);
	}

	@Test
	public void testForbidReferencesToLazy() {
		Bitser bitser = new Bitser();
		ReferenceOuterStruct original = new ReferenceOuterStruct();
		original.lazy = new SimpleLazyBits<>(new ReferenceInnerStruct());
		original.reference = original.lazy.get().target;
		original.lazy.get().reference = original.reference;

		String errorMessage = assertThrows(
				ReferenceBitserException.class, () -> bitser.stupidDeepCopy(original)
		).getMessage();
		assertContains(errorMessage, "-> reference");
		assertContains(errorMessage, "find unstable reference target");
	}

	@Test
	public void testForbidReferencesFromLazy() {
		Bitser bitser = new Bitser();
		ReferenceOuterStruct original = new ReferenceOuterStruct();
		original.lazy = new SimpleLazyBits<>(new ReferenceInnerStruct());
		original.reference = original.target;
		original.lazy.get().reference = original.reference;

		String errorMessage = assertThrows(
				ReferenceBitserException.class, () -> bitser.stupidDeepCopy(original)
		).getMessage();
		assertContains(errorMessage, "-> reference");
		assertContains(errorMessage, "find unstable reference target");
	}

	@BitStruct(backwardCompatible = true)
	private static class LazyList {

		@BitField(id = 0)
		final ArrayList<SimpleLazyBits<SimpleInnerStruct>> lazyList = new ArrayList<>();
	}

	@Test
	public void testLazyList() {
		Bitser bitser = new Bitser();

		LazyList list = new LazyList();
		list.lazyList.add(new SimpleLazyBits<>(new SimpleInnerStruct()));
		list.lazyList.add(new SimpleLazyBits<>(new SimpleInnerStruct()));
		list.lazyList.get(0).get().words.add("use");
		list.lazyList.get(1).get().words.add("multiple");

		LazyList incompatible = bitser.stupidDeepCopy(list);
		assertEquals("use", incompatible.lazyList.get(0).get().words.get(0));
		assertEquals("multiple", incompatible.lazyList.get(1).get().words.get(0));

		LazyList compatible = bitser.stupidDeepCopy(list);
		assertEquals("use", compatible.lazyList.get(0).get().words.get(0));
		assertEquals("multiple", compatible.lazyList.get(1).get().words.get(0));
	}

	@BitStruct(backwardCompatible = false)
	private static class UnknownGenericType {
		@SuppressWarnings("unused")
		SimpleLazyBits<?> lazyMystery;
	}

	@BitStruct(backwardCompatible = false)
	private static class IntGenericType {
		@SuppressWarnings("unused")
		SimpleLazyBits<Integer> lazyInt;
	}

	@BitStruct(backwardCompatible = false)
	private static class ListGenericType {
		@SuppressWarnings("unused")
		SimpleLazyBits<List<SimpleInnerStruct>> lazyList;
	}

	@Test
	public void testUnknownGenericType() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new UnknownGenericType())
		).getMessage();
		assertContains(errorMessage, "SimpleLazyBits<?>");
		assertContains(errorMessage, "TestSimpleLazyFieldWrapper$UnknownGenericType.lazyMystery");
	}

	@Test
	public void testIntegerGenericType() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new IntGenericType())
		).getMessage();
		assertContains(errorMessage, "must be a BitStruct");
		assertContains(errorMessage, "TestSimpleLazyFieldWrapper$IntGenericType.lazyInt");
	}

	@Test
	public void testListGenericType() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(new ListGenericType())
		).getMessage();
		assertContains(errorMessage, "must be a BitStruct");
		assertContains(errorMessage, "TestSimpleLazyFieldWrapper$ListGenericType.lazyList");
	}

	@BitStruct(backwardCompatible = false)
	static class UnexpectedAnnotation1 {

		@SuppressWarnings("unused")
		@ClassField(root = SimpleInnerStruct.class)
		SimpleLazyBits<SimpleInnerStruct> lazy;
	}

	@BitStruct(backwardCompatible = false)
	static class UnexpectedAnnotation2 {

		@SuppressWarnings("unused")
		@ReferenceField(stable = false, label = "lazy")
		SimpleLazyBits<SimpleInnerStruct> lazy;
	}

	@BitStruct(backwardCompatible = false)
	static class UnexpectedAnnotation3 {

		@SuppressWarnings("unused")
		@ReferenceFieldTarget(label = "lazy")
		SimpleLazyBits<SimpleInnerStruct> lazy;
	}

	private void assertUnexpectedAnnotation(Object subject) {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().hashCode(subject)
		).getMessage();
		assertContains(errorMessage, ".lazy");
		assertContains(errorMessage, "TestSimpleLazyFieldWrapper$UnexpectedAnnotation");
		assertContains(errorMessage, "SimpleLazyBits and ReferenceLazyBits fields shouldn't " +
				"have any bitser annotations except @BitField");
	}

	@Test
	public void testUnexpectedAnnotations() {
		assertUnexpectedAnnotation(new UnexpectedAnnotation1());
		assertUnexpectedAnnotation(new UnexpectedAnnotation2());
		assertUnexpectedAnnotation(new UnexpectedAnnotation3());
	}
}
