package com.github.knokko.bitser.io;

import com.github.knokko.bitser.RecursionNode;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 *     This class can be used to write individual bits to an {@link OutputStream}. It normally puts 8 bits into 1 byte.
 * </p>
 *
 * <p>
 *     This class allows bitser to conveniently write multiple properties to a single byte in an {@link OutputStream},
 *     which is crucial for serialization of small properties that need less than 8 bits.
 * </p>
 */
public class BitOutputStream {

	private final byte[] buffer = new byte[200];
	private final OutputStream byteStream;
	private int wipByte;
	private int bufferIndex;
	private int boolIndex;

	/**
	 * Constructs a new {@link BitOutputStream} that writes to {@code byteStream}
	 */
	public BitOutputStream(OutputStream byteStream) {
		this.byteStream = byteStream;
	}

	private void maybeFlushBuffer() throws IOException {
		if (bufferIndex == 200) {
			byteStream.write(buffer);
			bufferIndex = 0;
		}
	}

	private void flushCurrentByte() throws IOException {
		boolIndex = 0;
		buffer[bufferIndex++] = (byte) wipByte;
		wipByte = 0;
		maybeFlushBuffer();
	}

	/**
	 * Writes a single bit to this stream.
	 * @param value The next bit that should be written to this stream
	 * @throws IOException If the underlying {@link OutputStream} throws an IOException.
	 */
	public void write(boolean value) throws IOException {
		if (value) wipByte |= 1 << boolIndex;
		if (++boolIndex == 8) flushCurrentByte();
	}

	/**
	 * Writes a binary number of {@code numBits} bits to this stream.
	 * @param value The binary number to be written
	 * @param numBits The number of bits that should be used to write {@code value}. The value should be at least 0 and
	 *                smaller than {@code Math.pow(2, numBits)}.
	 * @throws IOException If the underlying {@link OutputStream} throws an IOException.
	 */
	public void write(int value, int numBits) throws IOException {
		wipByte |= value << boolIndex;
		boolIndex += numBits;
		if (boolIndex >= 8) {
			buffer[bufferIndex++] = (byte) wipByte;
			boolIndex -= 8;
			wipByte = value >> (numBits - boolIndex);
			maybeFlushBuffer();
		}
	}

	private void flushBuffer() throws IOException {
		if (boolIndex != 0) flushCurrentByte();
		if (bufferIndex != 0) byteStream.write(buffer, 0, bufferIndex);
		bufferIndex = 0;
	}

	/**
	 * Writes all bytes in {@code values} to this stream. For performance reasons, this method will try to use the
	 * {@link OutputStream#write(byte[])} method of the underlying {@link OutputStream}. Using this method will waste
	 * up to 7 bits.
	 * @param values The bytes to be written
	 * @throws IOException If the underlying {@link OutputStream} throws an IOException.
	 */
	public void write(byte[] values) throws IOException {
		flushBuffer();
		byteStream.write(values);
	}

	/**
	 * Flushes & closes this stream
	 * @throws IOException If the underlying {@link OutputStream} throws an IOException.
	 */
	public void finish() throws IOException {
		flushBuffer();
		byteStream.flush();
		byteStream.close();
	}

	/**
	 * This method is used by bitser to track how many bits are spent on each struct and field. It is also used for
	 * internal debugging purposes.
	 */
	public void pushContext(RecursionNode context, String fieldName) {}

	/**
	 * This method is used by bitser to track how many bits are spent on each struct and field. It is also used for
	 * internal debugging purposes.
	 */
	public void popContext(RecursionNode context, String fieldName) {}

	/**
	 * This method is used by bitser to track how many bits are spent on each struct and field. It is also used for
	 * internal debugging purposes.
	 */
	public void prepareProperty(String fieldName) {}

	/**
	 * This method is used by bitser to track how many bits are spent on each struct and field. It is also used for
	 * internal debugging purposes.
	 */
	public void finishProperty() {}

	/**
	 * This method is used by bitser to track how many bits and how much time is spent on each (de)serialization stage.
	 * It is also used for internal debugging purposes.
	 */
	public void setMarker(String marker) {}
}
