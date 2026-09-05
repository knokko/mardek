package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.field.IntegerField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = true)
public class TestFloatField {

	@BitField(optional = true, id = 10)
	@FloatField
	public Float optional;

	@BitField(id = 20)
	@FloatField(expectMultipleOf = 0.2)
	public double required;

	@Test
	public void testInvalidConfigurations() {
		assertThrows(InvalidBitFieldException.class, () -> new Bitser().stupidDeepCopy(new Invalid1()));
		assertThrows(InvalidBitFieldException.class, () -> new Bitser().stupidDeepCopy(new Invalid2()));
	}

	@Test
	public void testValidConfiguration() {
		Bitser bitser = new Bitser();
		this.optional = null;
		this.required = 1.8;

		TestFloatField loaded = bitser.stupidDeepCopy(this);
		assertNull(loaded.optional);
		assertEquals(1.8, loaded.required);

		this.optional = Float.NaN;
		this.required = -123.456;
		loaded = bitser.stupidDeepCopy(this);
		assertTrue(Float.isNaN(loaded.optional));
		assertEquals(this.required, loaded.required);
	}

	@Test
	public void testExpectMultipleOf() {
		Bitser bitser = new Bitser();

		this.optional = null;
		this.required = 1.4;

		byte[] bytes = bitser.toBytes(this);

		// This should fit in 2 bytes
		assertEquals(2, bytes.length);

		TestFloatField loaded = bitser.fromBytes(TestFloatField.class, bytes);
		assertNull(loaded.optional);
		assertEquals(1.4, loaded.required, 0.001);
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid1 {

		@BitField(optional = true)
		@FloatField
		@SuppressWarnings("unused")
		public float invalid;
	}

	@BitStruct(backwardCompatible = false)
	private static class Invalid2 {

		@FloatField
		@SuppressWarnings("unused")
		public int invalid;
	}

	@BitStruct(backwardCompatible = false)
	private static class CommonValues {

		@FloatField(commonValues = { 0.0, 2.0, 5.0 })
		float[] values;
	}

	@Test
	public void testCommonValues() {
		Bitser bitser = new Bitser();
		CommonValues original = new CommonValues();
		original.values = new float[] { -5f, -1f, 0.0001f, 2.1f, 3f, 5.0001f };

		CommonValues loaded = bitser.stupidDeepCopy(original);
		assertEquals(-5f, loaded.values[0]);
		assertEquals(-1f, loaded.values[1]);
		assertEquals(0f, loaded.values[2]); // Should be rounded to exactly 0
		assertEquals(2.1f, loaded.values[3]);
		assertEquals(3f, loaded.values[4]);
		assertEquals(5f, loaded.values[5]); // Should be rounded to exactly 5
	}

	@BitStruct(backwardCompatible = true)
	private static class ParticleSize {

		@BitField(id = 0)
		@FloatField(expectMultipleOf = 0.001)
		float growX;
	}

	@Test
	public void particleSizeRegressionTest() {
		ParticleSize original = new ParticleSize();
		ParticleSize copied = new Bitser().stupidDeepCopy(original, Bitser.BACKWARD_COMPATIBLE);
		assertEquals(0f, copied.growX);
	}

	@BitStruct(backwardCompatible = false)
	private static class OutOfRange {

		@FloatField(expectMultipleOf = 0.1, expectedIntegerMultiple = @IntegerField(
				maxValue = 100, expectUniform = false
		))
		float value;
	}

	@Test
	public void testIntegerMultipleOutOfRange() {
		OutOfRange original = new OutOfRange();
		original.value = 15f;

		assertEquals(15f, new Bitser().stupidDeepCopy(original).value);
	}
}
