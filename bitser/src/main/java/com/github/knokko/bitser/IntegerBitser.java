package com.github.knokko.bitser;

import com.github.knokko.bitser.exceptions.InvalidBitFieldException;
import com.github.knokko.bitser.exceptions.InvalidBitValueException;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;

import java.io.IOException;

import static java.lang.Long.max;

/**
 * This class contains the methods that bitser uses to (de)serialize integers. This is crucial for serializing
 * integer fields, as well as the lengths of string fields and collection fields, and even some floating-point fields.
 * This class is an important pillar of the bitser library.
 */
public class IntegerBitser {

	/**
	 * Determines the number of bits that would be needed to store a <i>uniform</i> integer with a minimum value of
	 * {@code minValue} and a maximum value of {@code maxValue}. The result of this method is
	 * {@code log2(1 + maxValue - minValue)}, computed in a way that is robust against integer overflow.
	 * @param minValue The minimum value that the potential integer can have
	 * @param maxValue The maximum value that the potential integer can have (must be at least {@code minValue})
	 */
	public static int requiredNumberOfBitsForEncoding(long minValue, long maxValue) {
		long maxRange = maxValue - minValue;

		// Overflow check ripped from Math.subtractExact
		boolean overflowed = ((maxValue ^ minValue) & (maxValue ^ maxRange)) < 0;
		if (overflowed) return 64;

		return 64 - Long.numberOfLeadingZeros(maxRange);
	}

	private static void check(long value, long minValue, long maxValue) {
		if (value < minValue || value > maxValue) {
			throw new InvalidBitValueException(value + " is out of range [" + minValue + ", " + maxValue + "]");
		}
	}

	/**
	 * <p>
	 *     Serializes the integer {@code value}, and writes its bits to {@code output}.
	 * </p>
	 * <p>
	 *     First, if {@code field.commonValues} is not empty, 1 bit will be stored to tell whether {@code value} is
	 * 	   one of the common values. If {@code value} is indeed one of the common values, its index into
	 * 	   {@code commonValues} will be stored using {@code log2(commonValues.length)} bits, after which this
	 * 	   method returns.
	 * </p>
	 * <p>
	 *     If {@code value} is <b>not</b> one of the {@code commonValues}, it will be serialized using
	 *     either {@link #encodeUniformInteger}, {@link #encodeVariableIntegerUsingTerminatorBits}, or
	 *     {@link #encodeDigitInteger} (depending on {@code field.digitSize} and {@code field.expectUniform}).
	 * </p>
	 * @param value The value to be serialized
	 * @param field The expected properties/distribution, which affects the serialization strategy
	 * @param output The output stream to which the bits should be written
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encodeInteger(
			long value, IntegerField.Properties field, BitOutputStream output
	) throws IOException {
		if (field.commonValues.length > 0) {
			int commonIndex = -1;
			for (int index = 0; index < field.commonValues.length; index++) {
				if (value == field.commonValues[index]) commonIndex = index;
			}

			output.write(commonIndex != -1);
			if (commonIndex != -1) {
				encodeUniformInteger(commonIndex, 0, field.commonValues.length - 1, output);
				return;
			}
		}
		if (field.expectUniform) {
			encodeUniformInteger(value, field.minValue, field.maxValue, output);
		} else {
			if (field.digitSize == IntegerField.DIGIT_SIZE_TERMINATORS) {
				encodeVariableIntegerUsingTerminatorBits(value, field.minValue, field.maxValue, output);
			} else if (field.digitSize >= 2 && field.digitSize <= 7) {
				encodeDigitInteger(value, field.minValue, field.maxValue, field.digitSize, output);
			} else {
				throw new InvalidBitFieldException("Unsupported digit size " + field.digitSize);
			}
		}
	}

	/**
	 * Encodes the length of a string or collection, using the default strategy for encoding non-negative
	 * variable-length integers.
	 * @param length The length/integer to be serialized
	 * @param output The output stream to which the bits should be written
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encodeUnknownLength(int length, BitOutputStream output) throws IOException {
		encodeVariableIntegerUsingTerminatorBits(length, 0L, Integer.MAX_VALUE, output);
	}

	/**
	 * Serializes {@code value} by simply writing all its 64 bits to {@code output}
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encodeFullLong(long value, BitOutputStream output) throws IOException {
		output.write((int) value & 255, 8);
		output.write((int) (value >> 8) & 255, 8);
		output.write((int) (value >> 16) & 255, 8);
		output.write((int) (value >> 24) & 255, 8);
		output.write((int) (value >> 32) & 255, 8);
		output.write((int) (value >> 40) & 255, 8);
		output.write((int) (value >> 48) & 255, 8);
		output.write((int) (value >> 56) & 255, 8);
	}

	/**
	 * Serializes {@code value} by simply writing all its 32 bits to {@code output}
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encodeFullInt(int value, BitOutputStream output) throws IOException {
		output.write(value & 255, 8);
		output.write((value >> 8) & 255, 8);
		output.write((value >> 16) & 255, 8);
		output.write((value >> 24) & 255, 8);
	}

	/**
	 * Serializes {@code value} using {@code log2(1 + maxValue - minValue)} bits. This is useful when {@code value} is
	 * expected to be (approximately) uniformly distributed between {@code minValue} and {@code maxValue}
	 * @param value The value to be serialized, which must be at least {@code minValue}, and at most {@code maxValue}
	 * @param minValue The minimum value that is allowed
	 * @param maxValue The maximum value that is allowed
	 * @param output The output stream to which the bits of {@code value} should be written
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encodeUniformInteger(long value, long minValue, long maxValue, BitOutputStream output) throws IOException {
		check(value, minValue, maxValue);

		int numBits = requiredNumberOfBitsForEncoding(minValue, maxValue);

		if (numBits == 64) {
			encodeFullLong(value, output);
			return;
		}

		value -= minValue;
		int remainingBits = numBits;
		for (; remainingBits > 8; remainingBits -= 8) {
			output.write((int) value & 255, 8);
			value >>= 8;
		}
		output.write((int) value & 255, remainingBits);
	}

	/**
	 * <p>
	 *     Serializes {@code value} using a variable-length encoding that is inspired by the decimal system. Smaller
	 *     values will be serialized using fewer bits than larger value, with the expectancy that smaller values
	 *     occur much more often.
	 * </p>
	 * <p>
	 *     In the decimal system, there are basically 11 characters: the digits 0 to 9, and a terminator character
	 *     to indicate that no more digits are coming (e.g. whitespace).
	 * </p>
	 * <p>
	 *     This digit-based encoding is a sort of generalization of the decimal system. It takes a {@code digitSize}
	 *     parameter than must be at least 2, and can be at most 7 (although digit sizes above 4 are rarely optimal).
	 *     It uses {@code (2^digitSize) - 1} 'digits' and 1 terminator character. Each 'digit' or terminator is stored
	 *     using {@code digitSize} bits.
	 * </p>
	 *
	 * <h3>Example: digitSize = 3</h3>
	 * <p>
	 *     When the digit size is 3 bits, we have {@code 2^3 - 1 = 7} digits and 1 terminator, so the largest digit is
	 *     6. For this example, assume that {@code minValue = 0}.
	 * </p>
	 * <ul>
	 *     <li>
	 *         To encode the value 0, we simply write the terminator, without any digits, which takes 3 bits.
	 *     </li>
	 *     <li>
	 *         To encode a value between 1 and 6, we write 1 digit followed by the terminator, which takes 6 bits.
	 *     </li>
	 *     <li>
	 *         To encode a value between 7 and 49, we write 2 digits followed by the terminator, which takes 9 bits.
	 *         The encoded value is {@code firstDigit + 7 * secondDigit}.
	 *     </li>
	 *     <li>
	 *         To encode a value between 49 and 342, we write 3 digits followed by the terminator, which takes 12 bits.
	 *         The encoded value is {@code firstDigit + 7 * secondDigit + 49 * thirdDigit}.
	 *     </li>
	 *     <li>Encoding 343 to 2,400 takes 15 bits</li>
	 *     <li>Encoding 2,401 to 16,806 takes 18 bits</li>
	 *     <li>Encoding 16,807 to 117,648 takes 21 bits</li>
	 *     <li>Encoding 117,649 to 823,542 takes 24 bits</li>
	 *     <li>Encoding 823,543 to 5,764,800 takes 27 bits</li>
	 *     <li>Encoding 5,764,801 to 40,353,606 takes 30 bits</li>
	 *     <li>Encoding 40,353,607 to 282,475,248 takes 33 bits</li>
	 *     <li>Encoding 282,475,249 to 1,977,326,742 takes 36 bits</li>
	 * </ul>
	 *
	 * <h3>Other digit sizes</h3>
	 * <ul>
	 *     <li>
	 *         Using a digit size of 2 bits is often more efficient to encode values that are not very large
	 *         (e.g. in the hundreds), but is rather inefficient for larger numbers because 1 out of its 4 bit
	 *         combinations is 'wasted' on the terminator bit. Let's again use {@code minValue = 0} for an example:
	 *         <ul>
	 *             <li>Encoding 0 takes 2 bits</li>
	 *             <li>Encoding 1 or 2 takes 4 bits</li>
	 *             <li>Encoding 3 to 8 takes 6 bits</li>
	 *             <li>Encoding 9 to 26 takes 8 bits</li>
	 *             <li>Encoding 27 to 80 takes 10 bits</li>
	 *             <li>Encoding 81 to 242 takes 12 bits</li>
	 *             <li>Encoding 243 to 728 takes 14 bits</li>
	 *             <li>Encoding 729 to 2186 takes 16 bits</li>
	 *             <li>Encoding 2,187 to 6,560 takes 18 bits</li>
	 *             <li>Encoding 6,551 to 19,682 takes 20 bits</li>
	 *             <li>Encoding 19,683 to 59,084 takes 22 bits</li>
	 *             <li>Encoding 59,085 to 117,146 takes 24 bits</li>
	 *         </ul>
	 *     </li>
	 *     <li>
	 *         Using a digit size of 4 bits is slightly more efficient to encode very large numbers (e.g. billions).
	 *         There are not many cases where using 4 bits is the best option.
	 *         <ul>
	 *             <li>Encoding 0 takes 4 bits</li>
	 *             <li>Encoding 1 to 14 takes 8 bits</li>
	 *             <li>Encoding 15 to 224 takes 12 bits</li>
	 *             <li>Encoding 225 to 3,374 takes 16 bits</li>
	 *             <li>Encoding 3,375 to 50,624 takes 20 bits</li>
	 *             <li>Encoding 50,625 to 759,374 takes 24 bits</li>
	 *             <li>Encoding 759,365 to 11,390,624 takes 28 bits</li>
	 *             <li>Encoding 11,390,625 to 170,859,374 takes 32 bits</li>
	 *             <li>Encoding 170,859,374 to 2,562,890,624 takes 36 bits</li>
	 *         </ul>
	 *     </li>
	 *     <li>
	 *         Using a digit size of 5 bits or more is almost never a good idea.
	 *     </li>
	 * </ul>
	 *
	 * <h3>When minValue > 0</h3>
	 * <p>
	 *     When {@code minValue > 0}, the {@code value} is subtracted by {@code minValue} before being serialized.
	 *     For instance, serializing value 5 with min. value 0 is equivalent to serializing value 8 with min. value 3.
	 * </p>
	 *
	 * <h3>When minValue < 0</h3>
	 * <p>
	 *     When {@code minValue < 0}, a sign bit will be placed in front of the number, to tell whether it is negative.
	 *     When {@code value < 0}, this method will basically serialize {@code -1 - value} (after the sign bit).
	 *     When {@code value >= 0}, this method will serialize {@code value} after the sign bit. For instance:
	 * </p>
	 * <ul>
	 *     <li>
	 *         The encoding of 4 with min. value -10 is a sign bit, followed by the encoding of 4 with min. value 0.
	 *     </li>
	 *     <li>
	 *         The encoding of -4 with min. value -10 is a sign bit, followed by the encoding of 3 with min. value 0.
	 *     </li>
	 *     <li>
	 *         The encoding of -4 with min. value -10 is nearly identical to the encoding of 3 with min. value -10:
	 *         only the sign bit differs.
	 *     </li>
	 * </ul>
	 *
	 * <h3>Exploiting maxValue</h3>
	 * <p>
	 *     This digit-based encoding can efficiently serialize small integers, even when {@code maxValue} is very
	 *     large (e.g. {@code Long.MAX_VALUE}). But, when a value close to {@code maxValue} is being serialized,
	 *     this encoding can sometimes omit the terminator.
	 * </p>
	 * <p>
	 *     Consider the case when {@code digitSize == 3 && maxValue == 40}. Since the maximum value of 40 can be
	 *     serialized using only 2 digits: {@code 1 * 5 + 7 * 5 = 40}, there is not a single value that would need
	 *     more than 2 digits, so the terminator is omitted for all 2-digit values.
	 * </p>
	 * @param value The value to be encoded/serialized
	 * @param minValue The minimum value that is allowed
	 * @param maxValue The maximum value that is allowed
	 * @param digitSize The digit size, in bits (must be at least 2, and at most 7)
	 * @param output The output stream to which the bits should be written
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encodeDigitInteger(long value, long minValue, long maxValue, int digitSize, BitOutputStream output) throws IOException {
		check(value, minValue, maxValue);
		int numDigits = (1 << digitSize) - 1;

		if (minValue < 0L && maxValue >= 0L) output.write(value >= 0L);
		long maxPositiveValue;
		if (value < 0L) {
			value = -(value + 1L);
			maxPositiveValue = -(minValue + 1L);
			if (maxValue < 0L) {
				value += maxValue + 1;
				maxPositiveValue += maxValue + 1;
			}
		} else {
			maxPositiveValue = maxValue;
			if (minValue > 0L) {
				value -= minValue;
				maxPositiveValue -= minValue;
			}
		}

		while (value > 0) {
			output.write((byte) (value % numDigits), digitSize);
			value /= numDigits;
			maxPositiveValue /= numDigits;
		}
		if (maxPositiveValue > 0L) {
			output.write((byte) numDigits, digitSize);
		}
	}

	/**
	 * Deserializes an integer that was encoded using {@link #encodeInteger}
	 * @param field The same integer properties used in {@link #encodeInteger}
	 * @param input The input stream from which the encoded bits can be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static long decodeInteger(IntegerField.Properties field, BitInputStream input) throws IOException {
		if (field.commonValues.length > 0 && input.read()) {
			int commonIndex = (int) decodeUniformInteger(0, field.commonValues.length - 1, input);
			return field.commonValues[commonIndex];
		}
		if (field.expectUniform) {
			return decodeUniformInteger(field.minValue, field.maxValue, input);
		} else {
			if (field.digitSize == IntegerField.DIGIT_SIZE_TERMINATORS) {
				return decodeVariableIntegerUsingTerminatorBits(field.minValue, field.maxValue, input);
			} else if (field.digitSize >= 2 && field.digitSize <= 7) {
				return decodeDigitInteger(field.minValue, field.maxValue, field.digitSize, input);
			} else {
				throw new InvalidBitFieldException("Unsupported digit size " + field.digitSize);
			}
		}
	}

	/**
	 * Deserializes an integer (length) using {@link #decodeInteger}. But, if
	 * {@code sizeLimit != null && result > sizeLimit.maxValue}, then an {@link InvalidBitValueException} is thrown.
	 * @param sizeField The integer properties used in {@link #encodeInteger}
	 * @param sizeLimit The collection size limit, or {@code null} when there is no limit
	 * @param description A description of the field whose length/size is being decoded. If this method throws an
	 *                    {@link InvalidBitValueException}, this description will be included in the error message.
	 * @param input The input stream from which the integer/length bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static int decodeLength(
			IntegerField.Properties sizeField, CollectionSizeLimit sizeLimit, String description, BitInputStream input
	) throws IOException, InvalidBitValueException {
		int length = (int) decodeInteger(sizeField, input);
		if (sizeLimit != null && length > sizeLimit.maxSize()) {
			throw new InvalidBitValueException(
					description + " " + length + " exceeds the size limit of " + sizeLimit.maxSize()
			);
		}
		return length;
	}

	/**
	 * Deserializes an integer (length) that was encoded using {@link #encodeUnknownLength}. But, if
	 * {@code sizeLimit != null && result > sizeLimit.maxValue}, then an {@link InvalidBitValueException} is thrown.
	 * @param sizeLimit The collection size limit, or {@code null} when there is no limit
	 * @param description A description of the field whose length/size is being decoded. If this method throws an
	 *                    {@link InvalidBitValueException}, this description will be included in the error message.
	 * @param input The input stream from which the integer/length bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static int decodeUnknownLength(
			CollectionSizeLimit sizeLimit, String description, BitInputStream input
	) throws IOException {
		int length = (int) decodeVariableIntegerUsingTerminatorBits(0L, Integer.MAX_VALUE, input);
		if (sizeLimit != null && length > sizeLimit.maxSize()) {
			throw new InvalidBitValueException(
					description + " " + length + " exceeds the size limit of " + sizeLimit.maxSize()
			);
		}
		return length;
	}

	/**
	 * Deserializes a <b>long</b>, by reading all its 64 bits. The <b>long</b> should have been encoded using
	 * {@link #encodeFullLong}
	 * @param input The input stream from which the bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static long decodeFullLong(BitInputStream input) throws IOException {
		return input.read(8) | ((long) input.read(8) << 8) | ((long) input.read(8) << 16) |
				((long) input.read(8) << 24) | ((long) input.read(8) << 32) |
				((long) input.read(8) << 40) | ((long) input.read(8) << 48) |
				((long) input.read(8) << 56);
	}

	/**
	 * Deserializes an <b>int</b>, by reading all its 32 bits. The <b>int</b> should have been encoded using
	 * {@link #encodeFullInt}
	 * @param input The input stream from which the bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static int decodeFullInt(BitInputStream input) throws IOException {
		return input.read(8) | (input.read(8) << 8) | (input.read(8) << 16) |
				(input.read(8) << 24);
	}

	/**
	 * Deserializes an integer that was serialized using {@link #encodeUniformInteger}
	 * @param minValue The same minimum value as used in {@link #encodeUniformInteger}
	 * @param maxValue The same maximum value as used in {@link #encodeUniformInteger}
	 * @param input The input stream from which the bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static long decodeUniformInteger(long minValue, long maxValue, BitInputStream input) throws IOException {
		int numBits = requiredNumberOfBitsForEncoding(minValue, maxValue);

		if (numBits == 64) return decodeFullLong(input);

		long value = 0;
		int startBit = 0;
		for (; startBit + 8 < numBits; startBit += 8) {
			value |= (long) (input.read(8)) << startBit;
		}
		value |= (long) (input.read(numBits - startBit)) << startBit;
		return value + minValue;
	}

	/**
	 * Deserializes an integer that was serialized/encoded using {@link #encodeDigitInteger}.
	 * @param minValue The same {@code minValue} that was used in {@link #encodeUniformInteger}
	 * @param maxValue The same {@code maxValue} that was used in {@link #encodeUniformInteger}
	 * @param digitSize The same {@code digitSize} that was used in {@link #encodeUniformInteger}
	 * @param input The input stream from which the bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static long decodeDigitInteger(
			long minValue, long maxValue, int digitSize, BitInputStream input
	) throws IOException {
		int numDigits = (1 << digitSize) - 1;

		boolean checkSign = minValue < 0L && maxValue >= 0L;
		boolean isPositive;
		long maxPositiveValue;
		if (checkSign) {
			if (input.read()) {
				isPositive = true;
				maxPositiveValue = maxValue;
			} else {
				isPositive = false;
				maxPositiveValue = -(minValue + 1L);
			}
		} else {
			isPositive = minValue >= 0L;
			maxPositiveValue = maxValue - minValue;
		}

		long value = 0L;
		long optimisticValue = 0L;
		long digitWeight = 1L;
		while (true) {
			if (optimisticValue >= maxPositiveValue) break;
			int nextDigit = input.read(digitSize);
			if (nextDigit == numDigits) break;

			value += digitWeight * nextDigit;

			long addOptimistic = digitWeight * (numDigits - 1);
			long oldOptimistic = optimisticValue;
			optimisticValue += addOptimistic;

			// If optimisticValue overflows, we know for sure that no more digits are needed
			if (optimisticValue < oldOptimistic || addOptimistic / digitWeight != numDigits - 1) break;

			digitWeight *= numDigits;
		}

		if (checkSign) {
			if (isPositive) return value;
			else return -value - 1L;
		} else {
			if (isPositive) return value + minValue;
			else return -value + maxValue;
		}
	}

	/**
	 * Decodes an integer that was serialized/encoded using {@link #encodeVariableIntegerUsingTerminatorBits}.
	 * @param minValue The same {@code minValue} that was used in {@link #encodeVariableIntegerUsingTerminatorBits}
	 * @param maxValue The same {@code maxValue} that was used in {@link #encodeVariableIntegerUsingTerminatorBits}
	 * @param input The input stream from which the bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static long decodeVariableIntegerUsingTerminatorBits(
			long minValue, long maxValue, BitInputStream input
	) throws IOException {
		int maxValueBits;
		if (minValue >= 0) {
			maxValueBits = requiredNumberOfBitsForEncoding(minValue, maxValue);
		} else if (maxValue <= 0) {
			maxValueBits = requiredNumberOfBitsForEncoding(-maxValue, minValue == Long.MIN_VALUE ? -(minValue + 1) : -minValue);
		} else {
			maxValueBits = requiredNumberOfBitsForEncoding(0, max(maxValue, -(minValue + 1)));
		}

		long result = 0;
		for (int bitCounter = 0; bitCounter < maxValueBits; bitCounter++) {
			if (bitCounter == 1 || bitCounter == 4 || bitCounter == 10 || bitCounter == 20 || bitCounter == 40) {
				if (!input.read()) break;
			}

			result <<= 1;
			if (input.read()) result |= 1;
		}

		if (minValue >= 0) {
			result += minValue;
		} else if (maxValue <= 0) {
			result = -result;
			result += maxValue;
		} else {
			if (input.read()) result = -result - 1;
		}

		if (result == -Long.MAX_VALUE && maxValue == 0 && minValue == Long.MIN_VALUE) {
			if (input.read()) return Long.MIN_VALUE;
		}

		return result;
	}

	/**
	 * <p>
	 *     Serializes {@code value} using a variable-length encoding that is uses 'terminator' bits at fixed positions.
	 *     Smaller values will be serialized using fewer bits than larger value, with the expectancy that smaller values
	 *     occur much more often. <b>This is the default variable-length integer encoding of bitser.</b>
	 * </p>
	 *
	 * <h3>When minValue = 0 and maxValue = Long.MAX_VALUE</h3>
	 * <p>
	 *     The terminator bit positions are 1, 4, 10, 20, and 40. The meaning of these positions is easiest to explain
	 *     when {@code minValue == 0 && maxValue == Long.MAX_VALUE}. Let {@code allBits} be the 63-bit binary
	 *     representation of {@code value}. The procedure is:
	 * </p>
	 * <ol>
	 *     <li>We write the first bit of {@code allBits}</li>
	 *     <li>
	 *         <ul>
	 *             <li>If all (remaining) bits of {@code allBits} are 0, we write 0, <b>and stop</b></li>
	 *             <li>If at least 1 remaining bit is 1, we write 1, <b>and continue</b></li>
	 *         </ul>
	 *     </li>
	 *     <li>We write bits 2, 3, and 4 of {@code allBits}</li>
	 *     <li>
	 *         <ul>
	 *             <li>If all (remaining) bits are 0, we write 0, and stop</li>
	 *             <li>If at least 1 remaining bit is 1, we write 1, and continue</li>
	 *         </ul>
	 *     </li>
	 *     <li>We write bits 5 to 10 (inclusive)</li>
	 *     <li>
	 *         <ul>
	 *             <li>If all (remaining) bits are 0...</li>
	 *             <li>If at least 1 remaining bit is 1...</li>
	 *         </ul>
	 *     </li>
	 *     <li>We write bits 11 to 20 (inclusive)</li>
	 *     <li>
	 *         <ul>
	 *             <li>If all (remaining) bits are 0...</li>
	 *             <li>If at least 1 remaining bit is 1...</li>
	 *         </ul>
	 *     </li>
	 *     <li>We write bits 21 to 40 (inclusive)</li>
	 *     <li>
	 *         <ul>
	 *             <li>If all (remaining) bits are 0...</li>
	 *             <li>If at least 1 remaining bit is 1...</li>
	 *         </ul>
	 *     </li>
	 *     <li>We write the remaining bits</li>
	 * </ol>
	 *
	 * Using this encoding, we:
	 * <ul>
	 *     <li>need 2 bits to encode 0 and 1</li>
	 *     <li>need 6 bits to encode 2 to 15</li>
	 *     <li>need 13 bits to encode 16 to 1,023</li>
	 *     <li>need 24 bits to encode 1,024 to 1,048,575</li>
	 *     <li>need 45 bits to encode 1,048,576 to pow(2, 40) - 1</li>
	 *     <li>need 68 bits to encode larger numbers</li>
	 * </ul>
	 *
	 * <h3>When maxValue < Long.MAX_VALUE</h3>
	 * <p>
	 *     When {@code maxValue < Long.MAX_VALUE}, the worst-case number of bits is decreased. For instance, when
	 *     {@code maxValue == 100}, we only need 7 bits in the worst case, since the binary representation of 100 is
	 *     0010011.
	 * </p>
	 *
	 * <h4>Example 1: value == 5</h4>
	 * <p>
	 *     When {@code value == 5}, whose binary representation is 1010000, we would serialize this value as
	 *     1<b><i>1</i></b>010<b><i>0</i></b>. The terminator bits are marked in bold and italic. The first terminator
	 *     bit is <b>1</b> because 1010000 has a non-zero bit after the first bit, but the second terminator bit is
	 *     <b>0</b> because 1010000 only has zero's after the fourth bit.
	 * </p>
	 *
	 * <h4>Example 2: value == 45</h4>
	 * <p>
	 *     When {@code value == 45}, whose binary representation is 1011010. We would serialize this value as
	 *     1<b><i>1</i></b>011<b><i>1</i></b>010, which are 9 bits. In this case, both terminator bits are <b>1</b>,
	 *     since there are non-zero bits after the fourth bit in 1011010. Since the binary representation of
	 *     {@code maxValue == 100} only takes 7 bits, this method will stop after writing 7 data bits and 2 terminator
	 *     bits, since all remaining bits are guaranteed to be 0.
	 * </p>
	 *
	 * <h3>When minValue > 0</h3>
	 * <p>
	 *     When {@code minValue > 0}, we basically shift everything by {@code minValue}: calling
	 *     {@code encodeVariableIntegerUsingTerminatorBits(value, 5, maxValue, output)} is equivalent to calling
	 *     {@code encodeVariableIntegerUsingTerminatorBits(value - 5, 0, maxValue - 5, output)}.
	 * </p>
	 *
	 * <h3>When minValue < 0 and maxValue <= 0</h3>
	 * <p>
	 *     When {@code minValue < 0 && maxValue <= 0}, we basically serialize {@code -value}, using the same method that
	 *     is used for serializing non-negative values. (Plus some special treatment for {@code Long.MIN_VALUE}.)
	 * </p>
	 *
	 * <h3>When minValue < 0 and maxValue > 0</h3>
	 * <p>
	 *     When {@code minValue < 0 && maxValue > 0}, we use an additional <i>sign</i> bit to tell whether {@code value}
	 *     is negative.
	 * </p>
	 * <ul>
	 *     <li>When {@code value >= 0}, we simply serialize the value</li>
	 *     <li>When {@code value < 0}, we serialize {@code -(value + 1)} instead</li>
	 * </ul>
	 * @param value The value to be encoded/serialized
	 * @param minValue The minimum value that is allowed
	 * @param maxValue The maximum value that is allowed
	 * @param output The output stream to which the bits should be written
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encodeVariableIntegerUsingTerminatorBits(
			long value, long minValue, long maxValue, BitOutputStream output
	) throws IOException {
		check(value, minValue, maxValue);

		boolean isNegative = value < 0;
		boolean wasMinValue = value == Long.MIN_VALUE;
		boolean wasAlmostMinValue = wasMinValue || value == Long.MIN_VALUE + 1;

		int maxValueBits;
		int valueBits;
		if (minValue >= 0) {
			maxValueBits = requiredNumberOfBitsForEncoding(minValue, maxValue);
			valueBits = requiredNumberOfBitsForEncoding(minValue, value);
			value -= minValue;
		} else if (maxValue <= 0) {
			if (value == Long.MIN_VALUE) value++;
			maxValueBits = requiredNumberOfBitsForEncoding(-maxValue, minValue == Long.MIN_VALUE ? -(minValue + 1) : -minValue);
			value = -value;
			valueBits = requiredNumberOfBitsForEncoding(-maxValue, value);
			value += maxValue;
		} else {
			maxValueBits = requiredNumberOfBitsForEncoding(0, max(maxValue, -(minValue + 1)));
			if (value < 0) value = -(value + 1);
			valueBits = requiredNumberOfBitsForEncoding(0, value);
		}

		int terminatorBit = maxValueBits;
		if (valueBits <= 1) terminatorBit = 1;
		else if (valueBits <= 4) terminatorBit = 4;
		else if (valueBits <= 10) terminatorBit = 10;
		else if (valueBits <= 20) terminatorBit = 20;
		else if (valueBits <= 40) terminatorBit = 40;

		if (terminatorBit > maxValueBits) terminatorBit = maxValueBits;

		int encodedBits = 0;
		for (int valueBit = terminatorBit - 1; valueBit >= 0; valueBit--) {
			output.write((value & (1L << valueBit)) != 0);

			encodedBits++;
			if (valueBit > 0 && (encodedBits == 1 || encodedBits == 4 || encodedBits == 10 || encodedBits == 20 || encodedBits == 40)) {
				output.write(true);
			}
		}

		if (terminatorBit < maxValueBits) output.write(false);
		if (minValue < 0 && maxValue > 0) output.write(isNegative);

		if (maxValue == 0 && minValue == Long.MIN_VALUE && wasAlmostMinValue) output.write(wasMinValue);
	}
}
