package com.github.knokko.bitser.test.wrapper;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.Bitser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.*;

@BitStruct(backwardCompatible = false)
public class TestIntegerField {

	@IntegerField(expectUniform = false, minValue = 10, maxValue = 100_000)
	private int varInt;

	@IntegerField(expectUniform = true, maxValue = 100)
	private int uniformInt;

	@Test
	public void testZero() {
		this.varInt = 10;
		TestIntegerField loaded = new Bitser().stupidDeepCopy(this);
		assertEquals(10, loaded.varInt);
		assertEquals(0, loaded.uniformInt);
	}

	@Test
	public void testNonZero() {
		this.varInt = 12345;
		this.uniformInt = -123456;
		TestIntegerField loaded = new Bitser().stupidDeepCopy(this);
		assertEquals(12345, loaded.varInt);
		assertEquals(-123456, loaded.uniformInt);
	}

	@Test
	public void testOutOfRange() {
		this.varInt = 9;
		String errorMessage = assertThrows(InvalidBitValueException.class,
				() -> new Bitser().stupidDeepCopy(this)
		).getMessage();
		assertContains(errorMessage, "9 is out of range [10, 100000]");
		assertContains(errorMessage, " -> varInt");
	}

	@BitStruct(backwardCompatible = false)
	private static class SingleShort {

		@IntegerField(expectUniform = true)
		Short value;
	}

	@Test
	public void testNumberOfShortBits() throws IOException {
		SingleShort instance = new SingleShort();
		instance.value = 123;

		Bitser bitser = new Bitser();
		BitCountStream counter = new BitCountStream();
		bitser.serialize(instance, counter);

		assertEquals(16, counter.getCounter());
	}

	@BitStruct(backwardCompatible = true)
	private static class DigitSizes {

		@BitField(id = 0)
		@IntegerField(expectUniform = false, digitSize = 2)
		final int[] digits2 = new int[1000];

		@BitField(id = 1)
		@IntegerField(expectUniform = false)
		final ArrayList<Integer> terminators = new ArrayList<>(1000);

		@BitField(id = 2)
		@IntegerField(expectUniform = false, digitSize = 3)
		final Integer[] digits3 = new Integer[1000];

		@BitField(id = 3)
		@IntegerField(expectUniform = false, digitSize = 4)
		final HashSet<Integer> digits4 = new HashSet<>();
	}

	@Test
	public void testDigitSizes() {
		DigitSizes sizes = new DigitSizes();
		for (int number = -500; number < 500; number++) {
			sizes.digits2[number + 500] = number;
			sizes.digits3[number + 500] = number;
			sizes.digits4.add(number);
			sizes.terminators.add(number);
		}

		Bitser bitser = new Bitser();
		DigitSizes incompatible = bitser.stupidDeepCopy(sizes);
		DigitSizes compatible = bitser.stupidDeepCopy(sizes, Bitser.BACKWARD_COMPATIBLE);

		for (int number = -500; number < 500; number++) {
			for (DigitSizes loaded : new DigitSizes[] { incompatible, compatible }) {
				assertEquals(number, loaded.digits2[number + 500]);
				assertEquals(number, loaded.digits3[number + 500]);
				assertEquals(number, loaded.terminators.get(number + 500));
				assertTrue(loaded.digits4.contains(number));
			}
		}
	}

	@BitStruct(backwardCompatible = true)
	private static class CommonValues {

		@BitField(id = 1234)
		@IntegerField(expectUniform = false, commonValues = { 1000L, -2000L, 3000L })
		final ArrayList<Integer> scores = new ArrayList<>();
	}

	@Test
	public void testCommonValues() {
		Bitser bitser = new Bitser();
		CommonValues common = new CommonValues();
		common.scores.add(0);
		common.scores.add(50);
		common.scores.add(1000);
		common.scores.add(2000);
		common.scores.add(-2000);
		common.scores.add(3000);

		CommonValues[] loadedValues = { bitser.stupidDeepCopy(common), bitser.stupidDeepCopy(common, Bitser.BACKWARD_COMPATIBLE) };
		for (CommonValues loaded : loadedValues) {
			assertEquals(0, loaded.scores.get(0));
			assertEquals(50, loaded.scores.get(1));
			assertEquals(1000, loaded.scores.get(2));
			assertEquals(2000, loaded.scores.get(3));
			assertEquals(-2000, loaded.scores.get(4));
			assertEquals(3000, loaded.scores.get(5));
		}
	}
}
