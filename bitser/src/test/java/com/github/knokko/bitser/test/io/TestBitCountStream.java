package com.github.knokko.bitser.test.io;

import com.github.knokko.bitser.io.BitCountStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBitCountStream {

	@Test
	public void testStartWithByteArray() {
		BitCountStream counter = new BitCountStream();
		counter.write(new byte[] { 1, 2, 3 });
		counter.write(true);
		counter.write(6, 2);

		assertEquals(24 + 1 + 2, counter.getCounter());
	}

	@Test
	public void testByteArrayInTheMiddle() {
		BitCountStream counter = new BitCountStream();
		counter.write(false);
		counter.write(new byte[] { 4, 5 });
		counter.write(9, 5);

		assertEquals(8 + 16 + 5, counter.getCounter());
	}

	@Test
	public void testEndWithByteArray() {
		BitCountStream counter = new BitCountStream();
		counter.write(5, 3);
		counter.write(true);
		counter.write(new byte[1]);

		assertEquals(8 + 8, counter.getCounter());
	}
}
