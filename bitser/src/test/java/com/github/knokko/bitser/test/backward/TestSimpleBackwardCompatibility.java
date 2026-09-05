package com.github.knokko.bitser.test.backward;

import com.github.knokko.bitser.BitEnum;
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.LegacyBitserException;
import com.github.knokko.bitser.field.*;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestSimpleBackwardCompatibility {

	@BitStruct(backwardCompatible = true)
	private static class Empty {

	}

	@Test
	public void testMinimal() {
		new Bitser().stupidDeepCopy(new Empty(), Bitser.BACKWARD_COMPATIBLE);
	}

	private enum OldPet {
		DOG,
		CAT
	}

	@SuppressWarnings("unused")
	private enum NewPet {
		OLD_DOG,
		OLD_CAT,
		CAT,
		DOG,
		PIG
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleBefore {

		@BitField(id = 0)
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
		int dummyChance;

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.5)
		float dummyFraction;

		@BitField(id = 5)
		String byeBye;

		@BitField(id = 4, optional = true)
		@EnumField(mode = BitEnum.Mode.Name)
		OldPet namedPet;

		@BitField(id = 6, optional = true)
		@EnumField(mode = BitEnum.Mode.Ordinal)
		OldPet ordinalPet;
	}

	@BitStruct(backwardCompatible = true)
	private static class SimpleAfter {

		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = -1)
		int weirdChance;

		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.01)
		float dummyFraction;

		@BitField(id = 10)
		@StableReferenceFieldId
		final UUID newID = UUID.randomUUID();

		@BitField(id = 4, optional = true)
		@EnumField(mode = BitEnum.Mode.Name)
		NewPet namedPet;

		@BitField(id = 6, optional = true)
		@EnumField(mode = BitEnum.Mode.Ordinal)
		NewPet ordinalPet;
	}

	@Test
	public void testSimpleBackwardCompatibility() {
		Bitser bitser = new Bitser();
		SimpleBefore before = new SimpleBefore();
		before.dummyChance = 12;
		before.dummyFraction = 2.5f;
		before.byeBye = "Bye bye";
		before.namedPet = OldPet.CAT;
		before.ordinalPet = OldPet.DOG;

		byte[] bytes1 = bitser.toBytes(before, Bitser.BACKWARD_COMPATIBLE);
		SimpleAfter after = bitser.fromBytes(SimpleAfter.class, bytes1, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(12, after.weirdChance);
		assertEquals(2.5f, after.dummyFraction);
		assertEquals(NewPet.CAT, after.namedPet);
		assertEquals(NewPet.OLD_DOG, after.ordinalPet);
		assertNotNull(after.newID);

		byte[] bytes2 = bitser.toBytes(after, Bitser.BACKWARD_COMPATIBLE);
		SimpleBefore back = bitser.fromBytes(SimpleBefore.class, bytes2, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(12, back.dummyChance);
		assertEquals(2.5f, back.dummyFraction);
		assertEquals(OldPet.CAT, back.namedPet);
		assertEquals(OldPet.DOG, back.ordinalPet);
		assertNull(back.byeBye);
	}

	@BitStruct(backwardCompatible = true)
	private static class NestedBefore {

		@BitField(id = 2)
		final SimpleBefore nested = new SimpleBefore();

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		int test = 45;

		@BitField(id = 3)
		String hello = "hi";

		@BitField(id = 4)
		@IntegerField(expectUniform = false)
		long ignored = 12345;
	}

	@BitStruct(backwardCompatible = true)
	private static class NestedAfter {

		@BitField(id = 2)
		final SimpleAfter nested = new SimpleAfter();

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		byte test = 22;

		@BitField(id = 3)
		String hello = "I";
	}

	@BitStruct(backwardCompatible = false)
	private static class NonBackwardNestedAfter {

		@BitField(id = 2)
		final SimpleAfter nested = new SimpleAfter();

		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		byte test = 22;

		@BitField(id = 3)
		String hello = "I";
	}

	@Test
	public void testNested() {
		Bitser bitser = new Bitser();

		NestedBefore before = new NestedBefore();
		before.nested.dummyChance = 99;
		before.nested.dummyFraction = 0.025f;
		before.nested.byeBye = "Hey";
		before.test += 1;
		before.hello = "world";
		before.ignored = -123;

		byte[] bytes1 = bitser.toBytes(before, Bitser.BACKWARD_COMPATIBLE);
		NestedAfter after = bitser.fromBytes(NestedAfter.class, bytes1, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(99, after.nested.weirdChance);
		assertEquals(0.025f, after.nested.dummyFraction);
		assertEquals(46, after.test);
		assertEquals("world", after.hello);

		byte[] bytes2 = bitser.toBytes(after, Bitser.BACKWARD_COMPATIBLE);
		NestedBefore back = bitser.fromBytes(NestedBefore.class, bytes2, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(99, back.nested.dummyChance);
		assertEquals(0.025f, back.nested.dummyFraction);
		assertEquals(46, back.test);
		assertEquals(12345, back.ignored);
	}

	@Test
	public void testConsistencyWithNonBackwardCompatibleStructs() {
		Bitser bitser = new Bitser();

		NestedAfter compatible = new NestedAfter();
		compatible.test = 90;
		compatible.hello = "me!";
		compatible.nested.dummyFraction = 0.75f;

		NonBackwardNestedAfter nope = bitser.fromBytes(
				NonBackwardNestedAfter.class, bitser.toBytes(compatible)
		);
		assertEquals(90, nope.test);
		assertEquals("me!", nope.hello);
		assertEquals(0.75f, nope.nested.dummyFraction);

		NestedAfter back = bitser.fromBytes(
				NestedAfter.class, bitser.toBytes(nope)
		);
		assertEquals(90, back.test);
		assertEquals("me!", back.hello);
		assertEquals(0.75f, back.nested.dummyFraction);
	}

	@BitStruct(backwardCompatible = true)
	private static class FullInt {

		@BitField(id = 0)
		@IntegerField(expectUniform = true)
		int x = 1_000_000;
	}

	@BitStruct(backwardCompatible = true)
	private static class PartialInt {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 200)
		int x = 150;
	}

	@BitStruct(backwardCompatible = true)
	private static class FullByte {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		@IntegerField(expectUniform = false)
		byte x;
	}

	@Test
	public void testInvalidFullIntToPartialInt() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				PartialInt.class, bitser.toBytes(new FullInt(), Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "value 1000000 is out of range");
		assertContains(errorMessage, "PartialInt.x");
	}

	@Test
	public void testInvalidPartialIntToFullByte() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				FullByte.class, bitser.toBytes(new PartialInt(), Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "value 150 is out of range");
		assertContains(errorMessage, "FullByte.x");
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalInt {

		@BitField(id = 0, optional = true)
		@IntegerField(expectUniform = false)
		Integer x = 10;
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalLong {

		@BitField(id = 0, optional = true)
		@IntegerField(expectUniform = true)
		Long x = 123456789L;
	}

	@Test
	public void testOptionalIntegers() {
		Bitser bitser = new Bitser();
		OptionalInt none = new OptionalInt();
		none.x = null;

		assertNull(bitser.fromBytes(
				OptionalLong.class, bitser.toBytes(none, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		).x);
		assertEquals(10L, bitser.fromBytes(
				OptionalLong.class, bitser.toBytes(new OptionalInt(), Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		).x);
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalFloat {

		@BitField(id = 2, optional = true)
		@FloatField
		Float x = 12f;
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalDouble {

		@BitField(id = 2, optional = true)
		@FloatField
		Double x = 100.0;
	}

	@Test
	public void testOptionalFloats() {
		Bitser bitser = new Bitser();
		OptionalFloat none = new OptionalFloat();
		none.x = null;

		assertNull(bitser.fromBytes(
				OptionalDouble.class, bitser.toBytes(none, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		).x);
	}

	@Test
	public void testDeserializeEmpty() {
		// Since OptionalFloat.x has id 2 and FullInt.x has id 0, the value of FullInt.x should be its default value
		Bitser bitser = new Bitser();
		assertEquals(1_000_000, bitser.fromBytes(FullInt.class, bitser.toBytes(
				new OptionalFloat(), Bitser.BACKWARD_COMPATIBLE
		), Bitser.BACKWARD_COMPATIBLE).x);
	}

	@Test
	public void testInvalidNamedEnumConversion() {
		Bitser bitser = new Bitser();
		SimpleAfter after = new SimpleAfter();
		after.namedPet = NewPet.PIG;

		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				SimpleBefore.class, bitser.toBytes(after, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "legacy enum constant PIG");
		assertContains(errorMessage, "SimpleBefore.namedPet");
	}

	@Test
	public void testInvalidOrdinalEnumConversion() {
		Bitser bitser = new Bitser();
		SimpleAfter after = new SimpleAfter();
		after.ordinalPet = NewPet.PIG;

		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				SimpleBefore.class, bitser.toBytes(after, Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "legacy ordinal 4");
		assertContains(errorMessage, "SimpleBefore.ordinalPet");
	}

	@BitStruct(backwardCompatible = true)
	private static class StringWrapper {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final String hello = "world";
	}

	@BitStruct(backwardCompatible = true)
	private static class IdWrapper {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final UUID id = UUID.randomUUID();
	}

	@Test
	public void testInvalidConversionFromStringToUUID() {
		Bitser bitser = new Bitser();
		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				IdWrapper.class, bitser.toBytes(new StringWrapper(), Bitser.BACKWARD_COMPATIBLE), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "from legacy string world");
		assertContains(errorMessage, "IdWrapper.id");
	}

	@BitStruct(backwardCompatible = true)
	private static class ParentWithIncompatibleChild {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final NonBackwardNestedAfter child = new NonBackwardNestedAfter();
	}

	@Test
	public void testBackwardCompatibleParentWithNonCompatibleChild() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(
						new ParentWithIncompatibleChild(), Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "is not backward compatible");
		assertContains(errorMessage, "NonBackwardNestedAfter");
	}

	@BitStruct(backwardCompatible = true)
	private static class ParentWithIncompatibleFunction {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		NonBackwardNestedAfter createChild() {
			return new NonBackwardNestedAfter();
		}
	}

	@Test
	public void testBackwardCompatibleParentWithNonCompatibleFunction() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(
					new ParentWithIncompatibleFunction(), Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "is not backward compatible");
		assertContains(errorMessage, "NonBackwardNestedAfter");
	}

	@BitStruct(backwardCompatible = false)
	private static class MissesID {

		@SuppressWarnings("unused")
		@BitField
		final String hello = "world";
	}

	@BitStruct(backwardCompatible = true)
	private static class ParentWithMissingIdChild {

		@SuppressWarnings("unused")
		@BitField(id = 0)
		final MissesID child = new MissesID();
	}

	@Test
	public void testBackwardCompatibleParentWithChildThatMissesID() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().toBytes(
					new ParentWithMissingIdChild(), Bitser.BACKWARD_COMPATIBLE
				)
		).getMessage();
		assertContains(errorMessage, "is not backward compatible");
		assertContains(errorMessage, "MissesID");
	}

	@Test
	public void testInvalidConversionFromStringToStruct() {
		Bitser bitser = new Bitser();
		StringWrapper old = new StringWrapper();
		byte[] bytes = bitser.toBytes(old, Bitser.BACKWARD_COMPATIBLE);

		String errorMessage = assertThrows(LegacyBitserException.class, () -> bitser.fromBytes(
				ParentWithMissingIdChild.class, bytes, Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "-> child");
		assertContains(errorMessage, "Can't convert from legacy string world to");
		assertContains(errorMessage, "TestSimpleBackwardCompatibility$MissesID");
	}
}
