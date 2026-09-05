package com.github.knokko.bitser;

import com.github.knokko.bitser.field.FloatField;
import com.github.knokko.bitser.io.BitCountStream;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.IOException;

import static com.github.knokko.bitser.IntegerBitser.*;
import static java.lang.Math.abs;

/**
 * This class contains the methods that bitser uses to (de)serialize floating-point numbers. This is crucial for any
 * {@link FloatField}.
 */
public class FloatBitser {

	/**
	 * Serializes the floating-point number {@code value}, and writes its data to {@code output}.
	 * <ol>
	 *     <li>
	 *         First, if {@code field.commonValues} is not empty, 1 bit will be stored to tell whether {@code value} is
	 *         one of the common values. If {@code value} is indeed one of the common values, its index into
	 *         {@code commonValues} will be stored using {@code log2(commonValues.length)} bits, after which this
	 *         method returns.
	 *     </li>
	 *     <li>
	 *         Next, if {@code expectMultipleOf} is specified (and non-zero), 1 bit will be stored to tell whether
	 *         {@code value} is an integer multiple of {@code expectMultipleOf}. If {@code value} is indeed an
	 *         integer multiple of {@code expectMultipleOf}, this integer multiple will be serialized using
	 *         {@link IntegerBitser}, after this method returns.
	 *     </li>
	 *     <li>
	 *         Finally, if this method hasn't returned yet, {@code value} will be serialized using the full 32 or 64
	 *         bits (depending on whether {@code doublePrecision = true}.
	 *     </li>
	 * </ol>
	 * @param value The floating-point number to be serialized. Note that all <b>float</b>s can safely be converted to
	 *              <b>double</b>s, without losing precision.
	 * @param doublePrecision Whether {@code value} should be stored using double-precision. This should be {@code true}
	 *                        for fields of type <b>double</b>, and {@code false} for fields of type <b>float</b>.
	 * @param field The properties of the float field, which affects the serialization strategy
	 * @param output The output stream to which the saved bits should be written
	 * @throws IOException When {@code output} throws an IOException.
	 */
	public static void encodeFloat(
			double value, boolean doublePrecision, FloatField.Properties field, BitOutputStream output
	) throws IOException {
		if (field.commonValues.length > 0) {
			for (int index = 0; index < field.commonValues.length; index++) {
				if (abs(value - field.commonValues[index]) <= field.errorTolerance) {
					output.prepareProperty("float-common");
					output.write(true);
					output.finishProperty();
					output.prepareProperty("float-common-index");
					encodeUniformInteger(index, 0, field.commonValues.length - 1, output);
					output.finishProperty();
					return;
				}
			}
			output.write(false);
		}
		if (field.expectMultipleOf != 0.0) {
			double doubleValue = ((Number) value).doubleValue();
			long count = Math.round(doubleValue / field.expectMultipleOf);
			double recoveredValue = count * field.expectMultipleOf;

			if (
					abs(recoveredValue - doubleValue) <= field.errorTolerance &&
							count >= field.expectedIntegerMultiple.minValue &&
							count <= field.expectedIntegerMultiple.maxValue
			) {
				BitCountStream counter = new BitCountStream();
				encodeInteger(count, field.expectedIntegerMultiple, counter);

				if ((!doublePrecision && counter.getCounter() < 32) || (doublePrecision && counter.getCounter() < 64)) {
					output.prepareProperty("float-simplified");
					output.write(true);
					output.finishProperty();
					output.prepareProperty("float-integer-value");
					encodeInteger(count, field.expectedIntegerMultiple, output);
					output.finishProperty();
					return;
				}
			}

			output.prepareProperty("float-simplified");
			output.write(false);
			output.finishProperty();
		}

		output.prepareProperty("float-value");
		if (doublePrecision) {
			encodeFullLong(Double.doubleToRawLongBits(value), output);
		} else {
			encodeFullInt(Float.floatToRawIntBits((float) value), output);
		}
		output.finishProperty();
	}

	/**
	 * Deserializes a floating-point number that was serialized using {@link #encodeFloat}.
	 * @param doublePrecision Whether {@code doublePrecision} was true in {@link #encodeFloat}
	 * @param field The same properties that were used in {@link #encodeFloat}
	 * @param input The input from which the bits should be read
	 * @return The deserialized floating-point value
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static double decodeFloat(
			boolean doublePrecision, FloatField.Properties field, BitInputStream input
	) throws IOException {
		if (field.commonValues.length > 0) {
			input.prepareProperty("float-common");
			boolean isCommon = input.read();
			input.finishProperty();

			if (isCommon) {
				input.prepareProperty("float-common-index");
				int commonIndex = (int) decodeUniformInteger(0, field.commonValues.length - 1, input);
				input.finishProperty();
				return field.commonValues[commonIndex];
			}
		}

		if (field.expectMultipleOf != 0.0) {
			input.prepareProperty("float-simplified");
			boolean isMultipleOf = input.read();
			input.finishProperty();

			if (isMultipleOf) {
				input.prepareProperty("float-integer-value");
				long count = IntegerBitser.decodeInteger(field.expectedIntegerMultiple, input);
				input.finishProperty();
				double result = count * field.expectMultipleOf;
				if (doublePrecision) return result;
				else return (float) result;
			}
		}

		input.prepareProperty("float-value");
		if (doublePrecision) {
			long doubleBits = decodeFullLong(input);
			input.finishProperty();
			return Double.longBitsToDouble(doubleBits);
		} else {
			int floatBits = decodeFullInt(input);
			input.finishProperty();
			return Float.intBitsToFloat(floatBits);
		}
	}
}
