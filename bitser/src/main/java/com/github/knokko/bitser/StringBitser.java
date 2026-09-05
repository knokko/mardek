package com.github.knokko.bitser;

import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import com.github.knokko.bitser.options.CollectionSizeLimit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.knokko.bitser.IntegerBitser.*;

/**
 * This class is used to (de)serialize strings.
 */
public class StringBitser {

	/**
	 * Serializes/encodes the given string. Currently, this basically uses UTF-8, but more (possibly custom) encodings
	 * may be added in the future.
	 * @param value The string to be serialized
	 * @param lengthField The properties that determine how the length of the string should be serialized
	 * @param output The output stream to which the bits should be written
	 * @throws IOException When {@code output} throws an IOException
	 */
	public static void encode(
			String value, IntegerField.Properties lengthField, BitOutputStream output
	) throws IOException {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

		output.prepareProperty("string-length");
		encodeInteger(bytes.length, lengthField, output);
		output.finishProperty();

		for (byte b : bytes) {
			output.prepareProperty("string-char");
			encodeUniformInteger(b, Byte.MIN_VALUE, Byte.MAX_VALUE, output);
			output.finishProperty();
		}
	}

	/**
	 * Deserializes/decodes a string that was serialized using {@link #encode}
	 * @param lengthField The same {@code lengthField} that was used in {@link #encode}
	 * @param sizeLimit When non-null, this is a limit on the length of the string (in bytes). When this maximum length
	 *                  is exceeded, an {@link com.github.knokko.bitser.exceptions.InvalidBitValueException} is thrown.
	 * @param input The input stream from which the bits should be read
	 * @throws IOException When {@code input} throws an IOException
	 */
	public static String decode(
			IntegerField.Properties lengthField, CollectionSizeLimit sizeLimit, BitInputStream input
	) throws IOException {
		input.prepareProperty("string-length");
		int length = decodeLength(lengthField, sizeLimit, "string length", input);
		input.finishProperty();

		byte[] bytes = new byte[length];
		for (int index = 0; index < length; index++) {
			input.prepareProperty("string-char");
			bytes[index] = (byte) decodeUniformInteger(Byte.MIN_VALUE, Byte.MAX_VALUE, input);
			input.finishProperty();
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
