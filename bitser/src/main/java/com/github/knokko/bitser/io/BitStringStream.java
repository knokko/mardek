package com.github.knokko.bitser.io;

/**
 * An implementation of {@link BitOutputStream} that writes all bits to a string containing only the characters '0' and
 * '1'. Note that this is a massive waste of memory/space (1 byte per bit), but it is convenient for unit tests, as well
 * as for some demonstrations. Note that this class currently does <b>not</b> support {@link #write(byte[])}.
 */
public class BitStringStream extends BitOutputStream {

	private final StringBuilder bitString = new StringBuilder();

	/**
	 * Constructs a new (initially empty) {@link BitStringStream}
	 */
	public BitStringStream() {
		super(null);
	}

	@Override
	public void write(boolean value) {
		bitString.append(value ? '1' : '0');
	}

	@Override
	public void write(int value, int numBits) {
		for (int bit = 0; bit < numBits; bit++) write((value & (1 << bit)) != 0);
	}

	@Override
	public void write(byte[] values) {
		throw new UnsupportedOperationException("This method is not (yet) supported");
	}

	@Override
	public void finish() {}

	/**
	 * Gets the string containing all '0's and '1's written so far
	 */
	public String get() {
		return bitString.toString();
	}
}
