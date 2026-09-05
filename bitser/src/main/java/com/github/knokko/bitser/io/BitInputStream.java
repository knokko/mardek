package com.github.knokko.bitser.io;

import com.github.knokko.bitser.RecursionNode;
import com.github.knokko.bitser.exceptions.UnexpectedBitserException;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;

/**
 * <p>
 *     This class can be used to read individual bits from an {@link InputStream}. It normally reads 8 bits from each
 *     byte.
 * </p>
 *
 * <p>
 *     This class allows bitser to conveniently read multiple properties from a single byte in an {@link InputStream},
 *     which is crucial for serialization of small properties that need less than 8 bits.
 * </p>
 */
public class BitInputStream {

	private final byte[] myBuffer = new byte[100];
	private final InputStream byteStream;
	private int bufferIndex;
	private int bufferLimit;
	private int currentByte;
	private int boolIndex = 8;

	/**
	 * Constructs a new {@link BitInputStream} that reads bits from {@code byteStream}
	 */
	public BitInputStream(InputStream byteStream) {
		this.byteStream = byteStream;
	}

	private void readNextByte() throws IOException {
		if (bufferIndex >= bufferLimit) {
			bufferLimit = byteStream.read(myBuffer);
			bufferIndex = 0;
			if (bufferLimit == -1) throw new IOException("End of stream reached");
		}
		currentByte = myBuffer[bufferIndex++] & 0xFF;
		boolIndex -= 8;
	}

	/**
	 * Reads a single bit from this stream.
	 * @return The next bit from this stream
	 * @throws IOException If the end of the stream has been reached, or the underlying {@link InputStream} throws an
	 * IOException.
	 */
	public boolean read() throws IOException {
		if (boolIndex == 8) readNextByte();
		return (currentByte & (1 << boolIndex++)) != 0;
	}

	/**
	 * Reads a binary number of {@code numBits} bits from this stream
	 * @param numBits The length of the binary number (in bits). It must be less than 8.
	 * @throws IOException If the end of the stream has been reached, or the underlying {@link InputStream} throws an
	 * IOException.
	 */
	public int read(int numBits) throws IOException {
		int value = (currentByte >> boolIndex) & ((1 << numBits) - 1);
		boolIndex += numBits;
		if (boolIndex > 8) {
			readNextByte();
			int remainingBits = boolIndex;
			int usedBits = numBits - remainingBits;
			value |= (currentByte & ((1 << remainingBits) - 1)) << usedBits;
		}
		return value;
	}

	/**
	 * Reads {@code destination.length} bytes from this stream. For performance reasons, this method will try to use the
	 * {@link InputStream#read(byte[])} of the underlying input stream. Using this method will discard/waste up to 7
	 * bits.
	 * @param destination The destination byte array, to which the bytes will be written
	 * @throws IOException If the end of the stream has been reached, or the underlying {@link InputStream} throws an
	 * IOException.
	 */
	public void read(byte[] destination) throws IOException {
		boolIndex = 8;

		int numReadBytes = min(bufferLimit - bufferIndex, destination.length);
		System.arraycopy(myBuffer, bufferIndex, destination, 0, numReadBytes);
		bufferIndex += numReadBytes;

		while (numReadBytes < destination.length) {
			int justReadBytes = byteStream.read(destination, numReadBytes, destination.length - numReadBytes);
			if (justReadBytes == -1) throw new IOException("End of stream reached");
			numReadBytes += justReadBytes;
			if (numReadBytes > destination.length) throw new UnexpectedBitserException("Too many bytes read?");
		}
	}

	/**
	 * Closes this stream, which simply closes the underlying {@link InputStream}
	 * @throws IOException If the underlying {@link InputStream} throws an IOException
	 */
	public void close() throws IOException {
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
