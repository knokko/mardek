package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.field.ReferenceFieldTarget;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

public class TestBitStruct {

	@BitStruct(backwardCompatible = false)
	private static class WandCooldowns {

		@IntegerField(minValue = 1, expectUniform = true)
		public int cooldown;

		@IntegerField(minValue = 1, expectUniform = true)
		private int maxCharges = 3;

		@IntegerField(minValue = 0, expectUniform = true)
		public Integer rechargeTime;
	}

	@Test
	public void testWandCooldowns() {
		WandCooldowns wandCooldowns = new WandCooldowns();
		wandCooldowns.cooldown = 5;
		wandCooldowns.maxCharges = 4;
		wandCooldowns.rechargeTime = 20;

		wandCooldowns = new Bitser().stupidDeepCopy(wandCooldowns);
		assertEquals(5, wandCooldowns.cooldown);
		assertEquals(4, wandCooldowns.maxCharges);
		assertEquals(20, wandCooldowns.rechargeTime);
	}

	@BitStruct(backwardCompatible = false)
	private static class Chain {

		@BitField(optional = true)
		public Chain next;

		@BitField
		public Properties properties;

		@BitStruct(backwardCompatible = false)
		public static class Properties {

			@BitField
			@IntegerField(expectUniform = false)
			public int strength;

			@SuppressWarnings("unused")
			private Properties() {
				this.strength = 0;
			}

			public Properties(int strength) {
				this.strength = strength;
			}
		}
	}

	@Test
	public void testChain() {
		Chain root = new Chain();
		root.properties = new Chain.Properties(10);
		root.next = new Chain();
		root.next.properties = new Chain.Properties(2);

		root = new Bitser().stupidDeepCopy(root);
		assertEquals(10, root.properties.strength);
		assertEquals(2, root.next.properties.strength);
		assertNull(root.next.next);
	}

	@Test
	public void testDeepEqualsAndHashCode() {
		Chain a = new Chain();
		Chain b = new Chain();

		Bitser bitser = new Bitser();
		assertFalse(bitser.deepEquals(a, null));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(null));
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.properties = new Chain.Properties(5);
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.properties = new Chain.Properties(5);
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.next = new Chain();
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		b.next = new Chain();
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.next.properties = new Chain.Properties(2);
		b.next.properties = new Chain.Properties(3);
		assertFalse(bitser.deepEquals(a, b));
		assertNotEquals(bitser.hashCode(a), bitser.hashCode(b));

		a.next.properties.strength = 3;
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));
	}

	@BitStruct(backwardCompatible = false)
	private static class NoAnnotations {

		@BitField
		@SuppressWarnings("unused")
		int nope;
	}

	@Test
	public void testNoAnnotationsError() {
		String errorMessage = assertThrows(InvalidBitFieldException.class,
				() -> new Bitser().serialize(new NoAnnotations(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "Missing annotations for");
	}

	private static class Entity {

		@IntegerField(expectUniform = false)
		int x;

		@SuppressWarnings("unused")
		long tempStuff;

		@IntegerField(expectUniform = true)
		int y;
	}

	private static class Rectangle extends Entity {

		@SuppressWarnings("unused")
		boolean temporary;

		@IntegerField(expectUniform = false, minValue = 1)
		int width;

		@IntegerField(expectUniform = true, minValue = 1)
		int height;
	}

	@BitStruct(backwardCompatible = false)
	private static class ColoredRectangle extends Rectangle {

		@BitField
		String color;
	}

	@Test
	public void testInheritance() {
		ColoredRectangle rectangle = new ColoredRectangle();
		rectangle.x = -12;
		rectangle.y = -34;
		rectangle.width = 56;
		rectangle.height = 789;
		rectangle.color = "red";

		rectangle = new Bitser().stupidDeepCopy(rectangle);
		assertEquals(-12, rectangle.x);
		assertEquals(-34, rectangle.y);
		assertEquals(56, rectangle.width);
		assertEquals(789, rectangle.height);
		assertEquals("red", rectangle.color);
	}

	static class SubWandCooldowns extends WandCooldowns {}

	@Test
	public void testNoBitStructInheritance() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class,
				() -> new Bitser().serialize(new SubWandCooldowns(), new BitCountStream())
		).getMessage();
		assertContains(errorMessage, "SubWandCooldowns is not a BitStruct");
	}

	@SuppressWarnings("unused")
	@BitStruct(backwardCompatible = false)
	private static class DuplicateIDs {

		@BitField(id = 0)
		String test1 = "hello";

		@BitField(id = 1, optional = true)
		String test2;

		@BitField(id = 1)
		String test3 = "world";
	}

	@Test
	public void testDuplicateIDs() {
		String errorMessage = assertThrows(
				InvalidBitFieldException.class, () -> new Bitser().stupidDeepCopy(new DuplicateIDs())
		).getMessage();
		assertContains(errorMessage, "DuplicateIDs");
		assertContains(errorMessage, "multiple @BitField");
		assertContains(errorMessage, "id 1");
	}

	@BitStruct(backwardCompatible = false)
	private static class ReferenceStruct {

		@ReferenceField(stable = false, label = "id")
		UUID id;
	}

	@Test
	public void testReferenceStructDeepEqualsAndHashCode() {
		ReferenceStruct a = new ReferenceStruct();
		a.id = new UUID(12, 34);
		ReferenceStruct b = new ReferenceStruct();
		b.id = new UUID(12, 34);

		Bitser bitser = new Bitser();
		assertFalse(bitser.deepEquals(a, b));

		a.id = b.id;
		assertTrue(bitser.deepEquals(a, b));
		assertEquals(bitser.hashCode(a), bitser.hashCode(b));
	}

	@BitStruct(backwardCompatible = true)
	private static class OptionalWithNonNullDefault {

		@BitField(id = 0, optional = true)
		@ReferenceFieldTarget(label = "ref")
		String simple = "optional";

		@BitField(id = 1, optional = true)
		@ReferenceField(stable = false, label = "ref")
		String reference = simple;
	}

	@Test
	public void testOptionalWithNonNullDefault() {
		var bitser = new Bitser();
		var original = new OptionalWithNonNullDefault();

		var copy1a = bitser.stupidDeepCopy(original);
		assertEquals("optional", copy1a.simple);
		assertSame(copy1a.simple, copy1a.reference);

		var copy1b = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("optional", copy1b.simple);
		assertSame(copy1b.simple, copy1b.reference);

		original.reference = null;

		var copy2a = bitser.stupidDeepCopy(original);
		assertEquals("optional", copy2a.simple);
		assertNull(copy2a.reference);

		var copy2b = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals("optional", copy2b.simple);
		assertNull(copy2b.reference);

		original.simple = null;

		var copy3a = bitser.stupidDeepCopy(original);
		assertNull(copy3a.simple);
		assertNull(copy3a.reference);

		var copy3b = bitser.stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertNull(copy3b.simple);
		assertNull(copy3b.reference);
	}
}
