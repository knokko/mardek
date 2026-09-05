package com.github.knokko.bitser.io;

import java.io.ByteArrayOutputStream;

/**
 * An implementation of {@link BitOutputStream} that counts how many bits are written, but does <b>not</b> store the
 * values of the bits. It is used internally by bitser to try out which encoding would be the most efficient.
 */
public class BitCountStream extends BitOutputStream {

	private int counter;

	public BitCountStream() {
		super(new ByteArrayOutputStream());
	}

	@Override
	public void write(boolean value) {
		counter += 1;
	}

	@Override
	public void write(int value, int numBits) {
		counter += numBits;
	}

	@Override
	public void write(byte[] bytes) {
		int bytesSoFar = counter / 8;
		if (8 * bytesSoFar != counter) counter = 8 * (1 + bytesSoFar);
		counter += 8 * bytes.length;
	}

	/**
	 * The number of bits that was written (so far)
	 */
	public int getCounter() {
		return counter;
	}
}
