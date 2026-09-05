package com.github.knokko.bitser.test;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.io.BitStringStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static com.github.knokko.bitser.IntegerBitser.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestIntegerBitser {

	@Test
	public void testRequiredNumberOfBitsForEncoding() {
		for (long offset : new long[]{Long.MIN_VALUE, -100, 0, 100, Long.MAX_VALUE - 4}) {
			assertEquals(1, requiredNumberOfBitsForEncoding(offset, offset + 1));
			assertEquals(2, requiredNumberOfBitsForEncoding(offset, offset + 2));
			assertEquals(2, requiredNumberOfBitsForEncoding(offset, offset + 3));
			assertEquals(3, requiredNumberOfBitsForEncoding(offset, offset + 4));
		}

		for (long value : new long[]{Long.MIN_VALUE, -100, 0, 100, Long.MAX_VALUE}) {
			assertEquals(0, requiredNumberOfBitsForEncoding(value, value));
		}

		assertEquals(63, requiredNumberOfBitsForEncoding(0, Long.MAX_VALUE));
		assertEquals(64, requiredNumberOfBitsForEncoding(-1, Long.MAX_VALUE));
		assertEquals(64, requiredNumberOfBitsForEncoding(Long.MIN_VALUE, Long.MAX_VALUE));

		assertEquals(63, requiredNumberOfBitsForEncoding(Long.MIN_VALUE, -1));
		assertEquals(64, requiredNumberOfBitsForEncoding(Long.MIN_VALUE, 0));
	}

	private String repeat(char c, int amount) {
		char[] chars = new char[amount];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	private String zeros(int amount) {
		return repeat('0', amount);
	}

	private String ones(int amount) {
		return repeat('1', amount);
	}

	@Test
	public void testEncodeUniformIntegerFullLongRange() {
		test("01" + zeros(62), output ->
				encodeUniformInteger(2, Long.MIN_VALUE, Long.MAX_VALUE, output)
		);
		test(ones(63) + "0", output ->
				encodeUniformInteger(Long.MAX_VALUE, -1, Long.MAX_VALUE, output)
		);
		test(ones(64), output ->
				encodeUniformInteger(-1, Long.MIN_VALUE, 10L, output)
		);
		test(zeros(63) + "1", output ->
				encodeUniformInteger(Long.MIN_VALUE, Long.MIN_VALUE, 100, output)
		);
	}

	@Test
	public void testEncodeUniformIntegerPartial() {
		test("001", output -> encodeUniformInteger(4, 0, 4, output));
		test("001", output -> encodeUniformInteger(4, 0, 7, output));
		test("001000000", output -> encodeUniformInteger(4, 0, 500, output));
		test("001", output -> encodeUniformInteger(-100, -104, -100, output));
		test("001", output -> encodeUniformInteger(-100, -104, -97, output));

		test("110", output -> encodeUniformInteger(3, 0, 4, output));
		test("110", output -> encodeUniformInteger(3, 0, 7, output));
		test("11", output -> encodeUniformInteger(3, 0, 3, output));
		test("11", output -> encodeUniformInteger(4, 1, 4, output));

		test(ones(63), output -> encodeUniformInteger(Long.MAX_VALUE, 0, Long.MAX_VALUE, output));
		test(zeros(63), output -> encodeUniformInteger(0, 0, Long.MAX_VALUE, output));
		test(ones(63), output -> encodeUniformInteger(-1, Long.MIN_VALUE, -1, output));
		test(zeros(63), output -> encodeUniformInteger(Long.MIN_VALUE, Long.MIN_VALUE, -1, output));

		test("", output -> encodeUniformInteger(10, 10, 10, output));
	}

	@Test
	public void testEncodePositiveVariableIntegerUsingTerminatorBits() {
		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(0, 0, 100, output));
		test("1 0", output -> encodeVariableIntegerUsingTerminatorBits(1, 0, 100, output));
		test("0 1 010 0", output -> encodeVariableIntegerUsingTerminatorBits(2, 0, 100, output));
		test("0 1 011 0", output -> encodeVariableIntegerUsingTerminatorBits(3, 0, 100, output));
		test("0 1 100 0", output -> encodeVariableIntegerUsingTerminatorBits(4, 0, 100, output));
		test("0 1 101 0", output -> encodeVariableIntegerUsingTerminatorBits(5, 0, 100, output));
		test("0 1 110 0", output -> encodeVariableIntegerUsingTerminatorBits(6, 0, 100, output));
		test("0 1 111 0", output -> encodeVariableIntegerUsingTerminatorBits(7, 0, 100, output));
		test("1 1 000 0", output -> encodeVariableIntegerUsingTerminatorBits(8, 0, 100, output));
		test("1 1 001 0", output -> encodeVariableIntegerUsingTerminatorBits(9, 0, 100, output));
		test("1 1 010 0", output -> encodeVariableIntegerUsingTerminatorBits(10, 0, 100, output));
		test("1 1 111 0", output -> encodeVariableIntegerUsingTerminatorBits(15, 0, 100, output));
		test("0 1 010 1 000", output -> encodeVariableIntegerUsingTerminatorBits(16, 0, 100, output));
		test("0 1 100 1 000", output -> encodeVariableIntegerUsingTerminatorBits(32, 0, 100, output));
		test("1 1 000 1 000", output -> encodeVariableIntegerUsingTerminatorBits(64, 0, 100, output));

		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(0, 0, 10, output));
		test("1 0", output -> encodeVariableIntegerUsingTerminatorBits(1, 0, 10, output));
		test("0 1 010", output -> encodeVariableIntegerUsingTerminatorBits(2, 0, 10, output));
		test("0 1 011", output -> encodeVariableIntegerUsingTerminatorBits(3, 0, 10, output));
		test("0 1 100", output -> encodeVariableIntegerUsingTerminatorBits(4, 0, 10, output));
		test("0 1 101", output -> encodeVariableIntegerUsingTerminatorBits(5, 0, 10, output));
		test("0 1 110", output -> encodeVariableIntegerUsingTerminatorBits(6, 0, 10, output));
		test("0 1 111", output -> encodeVariableIntegerUsingTerminatorBits(7, 0, 10, output));
		test("1 1 000", output -> encodeVariableIntegerUsingTerminatorBits(8, 0, 10, output));
		test("1 1 001", output -> encodeVariableIntegerUsingTerminatorBits(9, 0, 10, output));
		test("1 1 010", output -> encodeVariableIntegerUsingTerminatorBits(10, 0, 10, output));

		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(100, 100, 200, output));
		test("1 0", output -> encodeVariableIntegerUsingTerminatorBits(101, 100, 200, output));
		test("1 1 010", output -> encodeVariableIntegerUsingTerminatorBits(110, 100, 110, output));

		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(0, 0, Long.MAX_VALUE, output));
		test(ones(63 + 5), output -> encodeVariableIntegerUsingTerminatorBits(Long.MAX_VALUE, 0, Long.MAX_VALUE, output));
		test(ones(63 + 5 - 1) + "0", output -> encodeVariableIntegerUsingTerminatorBits(Long.MAX_VALUE - 1, 0, Long.MAX_VALUE, output));
	}

	@Test
	public void testEncodeNegativeVariableIntegerUsingTerminatorBits() {
		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(0, -100, 0, output));
		test("1 0", output -> encodeVariableIntegerUsingTerminatorBits(-1, -100, 0, output));
		test("0 1 010 0", output -> encodeVariableIntegerUsingTerminatorBits(-2, -100, 0, output));
		test("0 1 011 0", output -> encodeVariableIntegerUsingTerminatorBits(-3, -100, 0, output));
		test("0 1 100 0", output -> encodeVariableIntegerUsingTerminatorBits(-4, -100, 0, output));
		test("0 1 101 0", output -> encodeVariableIntegerUsingTerminatorBits(-5, -100, 0, output));
		test("0 1 110 0", output -> encodeVariableIntegerUsingTerminatorBits(-6, -100, 0, output));
		test("0 1 111 0", output -> encodeVariableIntegerUsingTerminatorBits(-7, -100, 0, output));
		test("1 1 000 0", output -> encodeVariableIntegerUsingTerminatorBits(-8, -100, 0, output));
		test("1 1 001 0", output -> encodeVariableIntegerUsingTerminatorBits(-9, -100, 0, output));
		test("1 1 010 0", output -> encodeVariableIntegerUsingTerminatorBits(-10, -100, 0, output));
		test("0 1 010 1 000", output -> encodeVariableIntegerUsingTerminatorBits(-16, -100, 0, output));
		test("0 1 100 1 000", output -> encodeVariableIntegerUsingTerminatorBits(-32, -100, 0, output));
		test("1 1 000 1 000", output -> encodeVariableIntegerUsingTerminatorBits(-64, -100, 0, output));

		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(0, -10, 0, output));
		test("1 0", output -> encodeVariableIntegerUsingTerminatorBits(-1, -10, 0, output));
		test("0 1 010", output -> encodeVariableIntegerUsingTerminatorBits(-2, -10, 0, output));
		test("0 1 011", output -> encodeVariableIntegerUsingTerminatorBits(-3, -10, 0, output));
		test("0 1 100", output -> encodeVariableIntegerUsingTerminatorBits(-4, -10, 0, output));
		test("0 1 101", output -> encodeVariableIntegerUsingTerminatorBits(-5, -10, 0, output));
		test("0 1 110", output -> encodeVariableIntegerUsingTerminatorBits(-6, -10, 0, output));
		test("0 1 111", output -> encodeVariableIntegerUsingTerminatorBits(-7, -10, 0, output));
		test("1 1 000", output -> encodeVariableIntegerUsingTerminatorBits(-8, -10, 0, output));
		test("1 1 001", output -> encodeVariableIntegerUsingTerminatorBits(-9, -10, 0, output));
		test("1 1 010", output -> encodeVariableIntegerUsingTerminatorBits(-10, -10, 0, output));

		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(-100, -200, -100, output));
		test("1 0", output -> encodeVariableIntegerUsingTerminatorBits(-101, -200, -100, output));
		test("1 1 010", output -> encodeVariableIntegerUsingTerminatorBits(-110, -110, -100, output));

		test("0 0", output -> encodeVariableIntegerUsingTerminatorBits(0, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5) + "1", output -> encodeVariableIntegerUsingTerminatorBits(Long.MIN_VALUE, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5) + "0", output -> encodeVariableIntegerUsingTerminatorBits(Long.MIN_VALUE + 1, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5 - 1) + "0", output -> encodeVariableIntegerUsingTerminatorBits(Long.MIN_VALUE + 2, Long.MIN_VALUE, 0, output));
		test(ones(63 + 5 - 2) + "01", output -> encodeVariableIntegerUsingTerminatorBits(Long.MIN_VALUE + 3, Long.MIN_VALUE, 0, output));
	}

	@Test
	public void testEncodeVariableIntegerAroundZero() {
		test("0 00", output -> encodeVariableIntegerUsingTerminatorBits(0, -10, 10, output));
		test("1 00", output -> encodeVariableIntegerUsingTerminatorBits(1, -10, 10, output));
		test("0 01", output -> encodeVariableIntegerUsingTerminatorBits(-1, -10, 10, output));
		test("1 01", output -> encodeVariableIntegerUsingTerminatorBits(-2, -10, 10, output));
		test("0 00", output -> encodeVariableIntegerUsingTerminatorBits(0, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("1 00", output -> encodeVariableIntegerUsingTerminatorBits(1, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("0 01", output -> encodeVariableIntegerUsingTerminatorBits(-1, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("1 01", output -> encodeVariableIntegerUsingTerminatorBits(-2, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("0 00", output -> encodeVariableIntegerUsingTerminatorBits(0, Long.MIN_VALUE, Long.MAX_VALUE, output));
		test("1 00", output -> encodeVariableIntegerUsingTerminatorBits(1, -1, Long.MAX_VALUE, output));
		test("0 01", output -> encodeVariableIntegerUsingTerminatorBits(-1, -10, Long.MAX_VALUE, output));
		test("1 01", output -> encodeVariableIntegerUsingTerminatorBits(-2, Long.MIN_VALUE, 1, output));

		test("0 1 010 0", output -> encodeVariableIntegerUsingTerminatorBits(2, -10, 10, output));
		test("0 1 011 0", output -> encodeVariableIntegerUsingTerminatorBits(3, -10, 10, output));
		test("0 1 100 0", output -> encodeVariableIntegerUsingTerminatorBits(4, -10, 10, output));
		test("0 1 101 0", output -> encodeVariableIntegerUsingTerminatorBits(5, -1, 10, output));
		test("0 1 110 0", output -> encodeVariableIntegerUsingTerminatorBits(6, -10, 10, output));
		test("0 1 111 0", output -> encodeVariableIntegerUsingTerminatorBits(7, -10, 10, output));
		test("1 1 000 0", output -> encodeVariableIntegerUsingTerminatorBits(8, -10, 10, output));
		test("1 1 001 0", output -> encodeVariableIntegerUsingTerminatorBits(9, -10, 10, output));
		test("1 1 010 0", output -> encodeVariableIntegerUsingTerminatorBits(10, -1, 10, output));

		test("0 1 010 1", output -> encodeVariableIntegerUsingTerminatorBits(-3, -10, 10, output));
		test("0 1 011 1", output -> encodeVariableIntegerUsingTerminatorBits(-4, -10, 10, output));
		test("0 1 100 1", output -> encodeVariableIntegerUsingTerminatorBits(-5, -10, 1, output));
		test("0 1 101 1", output -> encodeVariableIntegerUsingTerminatorBits(-6, -10, 10, output));
		test("0 1 110 1", output -> encodeVariableIntegerUsingTerminatorBits(-7, -10, 10, output));
		test("0 1 111 1", output -> encodeVariableIntegerUsingTerminatorBits(-8, -10, 10, output));
		test("1 1 000 1", output -> encodeVariableIntegerUsingTerminatorBits(-9, -10, 10, output));
		test("1 1 001 1", output -> encodeVariableIntegerUsingTerminatorBits(-10, -10, 1, output));
	}

	@Test
	public void testEncodePositiveIntegerUsingDigitSize2() {
		test("11", output -> encodeDigitInteger(0, 0, 50, 2, output));
		test("10 11", output -> encodeDigitInteger(1, 0, 50, 2, output));
		test("01 11", output -> encodeDigitInteger(2, 0, 50, 2, output));
		test("00 10 11", output -> encodeDigitInteger(3, 0, 50, 2, output));
		test("10 10 11", output -> encodeDigitInteger(4, 0, 50, 2, output));
		test("01 10 11", output -> encodeDigitInteger(5, 0, 50, 2, output));
		test("00 01 11", output -> encodeDigitInteger(6, 0, 50, 2, output));
		test("10 01 11", output -> encodeDigitInteger(7, 0, 50, 2, output));
		test("01 01 11", output -> encodeDigitInteger(8, 0, 50, 2, output));
		test("00 00 10 11", output -> encodeDigitInteger(9, 0, 50, 2, output));
		test("10 00 10 11", output -> encodeDigitInteger(10, 0, 50, 2, output));
		test("00 01 10 11", output -> encodeDigitInteger(15, 0, 50, 2, output));
		test("10 01 10 11", output -> encodeDigitInteger(16, 0, 50, 2, output));
		test("01 01 01 11", output -> encodeDigitInteger(26, 0, 50, 2, output));
		test("00 00 00 10", output -> encodeDigitInteger(27, 0, 50, 2, output));
		test("10 00 00 10", output -> encodeDigitInteger(28, 0, 50, 2, output));
		test("01 10 00 10", output -> encodeDigitInteger(32, 0, 50, 2, output));
		test("01 10 01 10", output -> encodeDigitInteger(50, 0, 50, 2, output));

		test("11", output -> encodeDigitInteger(0, 0, 10, 2, output));
		test("10 11", output -> encodeDigitInteger(1, 0, 10, 2, output));
		test("01 11", output -> encodeDigitInteger(2, 0, 10, 2, output));
		test("00 10 11", output -> encodeDigitInteger(3, 0, 10, 2, output));
		test("10 10 11", output -> encodeDigitInteger(4, 0, 10, 2, output));
		test("01 10 11", output -> encodeDigitInteger(5, 0, 10, 2, output));
		test("00 01 11", output -> encodeDigitInteger(6, 0, 10, 2, output));
		test("10 01 11", output -> encodeDigitInteger(7, 0, 10, 2, output));
		test("01 01 11", output -> encodeDigitInteger(8, 0, 10, 2, output));
		test("00 00 10", output -> encodeDigitInteger(9, 0, 10, 2, output));
		test("10 00 10", output -> encodeDigitInteger(10, 0, 10, 2, output));

		test("11", output -> encodeDigitInteger(100, 100, 200, 2, output));
		test("10 11", output -> encodeDigitInteger(101, 100, 200, 2, output));
		test("10 00 10", output -> encodeDigitInteger(110, 100, 110, 2, output));

		test("11", output -> encodeDigitInteger(0, 0, Long.MAX_VALUE, 2, output));
		test("11", output -> encodeDigitInteger(12345, 12345, Long.MAX_VALUE, 2, output));
	}

	@Test
	public void testEncodeNegativeIntegerUsingDigitSize2() {
		test("1", output -> encodeDigitInteger(0, -20, 0, 2, output));
		test("0 11", output -> encodeDigitInteger(-1, -20, 0, 2, output));
		test("0 10 11", output -> encodeDigitInteger(-2, -20, 0, 2, output));
		test("0 01 11", output -> encodeDigitInteger(-3, -20, 0, 2, output));
		test("0 00 10 11", output -> encodeDigitInteger(-4, -20, 0, 2, output));
		test("0 10 10 11", output -> encodeDigitInteger(-5, -20, 0, 2, output));
		test("0 01 01 11", output -> encodeDigitInteger(-9, -20, 0, 2, output));
		test("0 00 00 10", output -> encodeDigitInteger(-10, -20, 0, 2, output));
		test("0 01 01 10", output -> encodeDigitInteger(-18, -20, 0, 2, output));
		test("0 10 00 01", output -> encodeDigitInteger(-20, -20, 0, 2, output));

		test("11", output -> encodeDigitInteger(-1, -20, -1, 2, output));
		test("10 11", output -> encodeDigitInteger(-2, -20, -1, 2, output));
		test("01 11", output -> encodeDigitInteger(-3, -20, -1, 2, output));
		test("01 01 10", output -> encodeDigitInteger(-18, -20, -1, 2, output));
		test("10 00 01", output -> encodeDigitInteger(-20, -20, -1, 2, output));

		test("11", output -> encodeDigitInteger(-100, -200, -100, 2, output));
		test("10 11", output -> encodeDigitInteger(-101, -200, -100, 2, output));
	}

	@Test
	public void testEncodeIntegerAroundZeroDigitSize2() {
		test("1 11", output -> encodeDigitInteger(0, -10, 10, 2, output));
		test("1 10 11", output -> encodeDigitInteger(1, -10, 10, 2, output));
		test("0 11", output -> encodeDigitInteger(-1, -10, 10, 2, output));
		test("0 10 11", output -> encodeDigitInteger(-2, -10, 10, 2, output));

		test("1 11", output -> encodeDigitInteger(0, Long.MIN_VALUE, Long.MAX_VALUE, 2, output));
		test("1 10 11", output -> encodeDigitInteger(1, Long.MIN_VALUE, Long.MAX_VALUE, 2, output));
		test("0 11", output -> encodeDigitInteger(-1, Long.MIN_VALUE, Long.MAX_VALUE, 2, output));
		test("0 10 11", output -> encodeDigitInteger(-2, Long.MIN_VALUE, Long.MAX_VALUE, 2, output));

		test("1 10 11", output -> encodeDigitInteger(1, -1, Long.MAX_VALUE, 2, output));
		test("0 11", output -> encodeDigitInteger(-1, -10, Long.MAX_VALUE, 2, output));
		test("0 10 11", output -> encodeDigitInteger(-2, Long.MIN_VALUE, 1, 2, output));

		test("1 01 11", output -> encodeDigitInteger(2, -10, 10, 2, output));
		test("1 00 10 11", output -> encodeDigitInteger(3, -10, 10, 2, output));
		test("1 10 10 11", output -> encodeDigitInteger(4, -10, 10, 2, output));
		test("1 01 10 11", output -> encodeDigitInteger(5, -1, 10, 2, output));

		test("0 00 00 10", output -> encodeDigitInteger(-10, -10, 1, 2, output));
	}

	private void test(String expected, ThrowingConsumer<BitOutputStream> encoder) {
		BitStringStream bitStream = new BitStringStream();
		try {
			encoder.accept(bitStream);
		} catch (Throwable e) {
			throw new AssertionError(e);
		}

		assertEquals(expected.replaceAll(" ", ""), bitStream.get());
	}

	@Test
	public void testDecode() throws IOException {
		for (long value = 0; value <= 100; value++) {
			test(value, 0, 100);
			test(value, 0, 10_000);
			test(value, 0, Long.MAX_VALUE);
		}
		for (long value = -100; value <= 0; value++) {
			test(value, -100, 0);
			test(value, -10_000, 0);
			test(value, Long.MIN_VALUE, 0);
		}
		for (long value = -100; value <= 100; value++) {
			test(value, -100, 100);
			test(value, -10_000, 10_000);
			test(value, Long.MIN_VALUE, Long.MAX_VALUE);
		}
		for (long value = -8000; value <= -7000; value++) {
			test(value, -8000, -7000);
			test(value, -100_000, -7000);
			test(value, -100_000, -2000);
			test(value, Long.MIN_VALUE, Long.MAX_VALUE);
		}
		for (long value = 7000; value <= 8000; value++) {
			test(value, 7000, 8000);
			test(value, 7000, 100_000);
			test(value, 2000, 100_000);
			test(value, Long.MIN_VALUE, Long.MAX_VALUE);
		}

		for (long minValue : new long[]{-100, 0}) {
			for (long value = 0; value <= 100_000; value++) test(value, minValue, Long.MAX_VALUE);
			test(Long.MAX_VALUE, minValue, Long.MAX_VALUE);
		}

		for (long maxValue : new long[]{0, 100}) {
			for (long value = -100_000; value <= 0; value++) test(value, Long.MIN_VALUE, maxValue);
			test(Long.MIN_VALUE, Long.MIN_VALUE, maxValue);
		}

		for (long candidate : new long[]{
				Long.MIN_VALUE, Long.MIN_VALUE + 1, Long.MIN_VALUE + 2, -100, -1, 0,
				Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE - 2, 100, 1
		}) {
			test(candidate, Long.MIN_VALUE, Long.MAX_VALUE);
			if (candidate != Long.MIN_VALUE) test(candidate, Long.MIN_VALUE + 1, Long.MAX_VALUE);
			if (candidate < 0) {
				test(candidate, Long.MIN_VALUE, 0);
				if (candidate != Long.MIN_VALUE) test(candidate, Long.MIN_VALUE + 1, 0);
			}
			if (candidate > 0) {
				test(candidate, 0, Long.MAX_VALUE);
				if (candidate != Long.MAX_VALUE) test(candidate, 0, Long.MAX_VALUE - 1);
			}
		}
	}

	private void test(long value, long minValue, long maxValue) throws IOException {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);
		encodeVariableIntegerUsingTerminatorBits(value, minValue, maxValue, bitOutput);
		encodeDigitInteger(value, minValue, maxValue, 2, bitOutput);
		encodeDigitInteger(value, minValue, maxValue, 3, bitOutput);
		encodeDigitInteger(value, minValue, maxValue, 4, bitOutput);
		encodeUniformInteger(value, minValue, maxValue, bitOutput);
		encodeDigitInteger(value, minValue, maxValue, 5, bitOutput);
		encodeDigitInteger(value, minValue, maxValue, 6, bitOutput);
		encodeDigitInteger(value, minValue, maxValue, 7, bitOutput);
		bitOutput.finish();

		BitInputStream bitInput = new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray()));
		assertEquals(value, decodeVariableIntegerUsingTerminatorBits(minValue, maxValue, bitInput));
		assertEquals(value, decodeDigitInteger(minValue, maxValue, 2, bitInput));
		assertEquals(value, decodeDigitInteger(minValue, maxValue, 3, bitInput));
		assertEquals(value, decodeDigitInteger(minValue, maxValue, 4, bitInput));
		assertEquals(value, decodeUniformInteger(minValue, maxValue, bitInput));
		assertEquals(value, decodeDigitInteger(minValue, maxValue, 5, bitInput));
		assertEquals(value, decodeDigitInteger(minValue, maxValue, 6, bitInput));
		assertEquals(value, decodeDigitInteger(minValue, maxValue, 7, bitInput));
		bitInput.close();
	}
}
