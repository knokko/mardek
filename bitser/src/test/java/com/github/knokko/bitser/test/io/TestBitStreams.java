package com.github.knokko.bitser.test.io;

import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.Bitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static com.github.knokko.bitser.test.wrapper.TestHelper.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestBitStreams {

	@Test
	public void testByteCount() throws IOException {
		testByteCount(new boolean[0], 0);
		testByteCount(new boolean[1], 1);
		testByteCount(new boolean[7], 1);
		testByteCount(new boolean[8], 1);
		testByteCount(new boolean[9], 2);
		testByteCount(new boolean[16], 2);
		testByteCount(new boolean[17], 3);
	}

	private void testByteCount(boolean[] bits, int expectedCount) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		BitOutputStream bitStream = new BitOutputStream(byteStream);

		for (boolean bit : bits) bitStream.write(bit);
		bitStream.finish();

		assertEquals(expectedCount, byteStream.toByteArray().length);
	}

	@Test
	public void testCompatibility() throws IOException {
		testCompatibility(new boolean[0]);
		testCompatibility(new boolean[]{false});
		testCompatibility(new boolean[]{true});

		testCompatibility(new boolean[]{true, false, false, true, true, false, true});
		testCompatibility(new boolean[]{true, false, false, true, true, false, true, true});
		testCompatibility(new boolean[]{true, false, false, true, true, false, true, true, false});

		boolean[] array = new boolean[1000];
		testCompatibility(array);
		Arrays.fill(array, true);
		testCompatibility(array);

		Random rng = new Random(123456);
		for (int index = 0; index < array.length; index++) array[index] = rng.nextBoolean();
		testCompatibility(array);
	}

	private void testCompatibility(boolean[] bits) throws IOException {
		ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
		BitOutputStream outputBits = new BitOutputStream(outputBytes);

		for (boolean bit : bits) outputBits.write(bit);
		outputBits.finish();

		BitInputStream inputBits = new BitInputStream(new ByteArrayInputStream(outputBytes.toByteArray()));
		for (boolean bit : bits) assertEquals(bit, inputBits.read());
		inputBits.close();
	}

	@BitStruct(backwardCompatible = false)
	private static class SmallStruct {

		@BitField
		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true)
		final int x = 123;
	}

	@BitStruct(backwardCompatible = true)
	private static class LargerStruct {

		@BitField(id = 5)
		@SuppressWarnings("unused")
		@IntegerField(expectUniform = true)
		final long y = -1234L;
	}

	@Test
	public void testEndOfStream1() {
		Bitser bitser = new Bitser();
		byte[] bytes = bitser.toBytes(new SmallStruct());
		String errorMessage = assertThrows(
				IllegalArgumentException.class, () -> bitser.fromBytes(LargerStruct.class, bytes)
		).getMessage();
		assertContains(errorMessage, "too short");
	}

	@Test
	public void testEndOfStream2() {
		Bitser bitser = new Bitser();
		byte[] bytes = bitser.toBytes(new SmallStruct());
		String errorMessage = assertThrows(IOException.class, () -> bitser.deserialize(
				LargerStruct.class, new BitInputStream(new ByteArrayInputStream(bytes))
		)).getMessage();
		assertContains(errorMessage, "IO exception");
		assertContains(errorMessage, "-> y");
	}

	@Test
	public void testEndOfStream3() {
		String errorMessage = assertThrows(IOException.class, () -> new Bitser().deserialize(
				LargerStruct.class, new BitInputStream(new ByteArrayInputStream(new byte[0]))
		)).getMessage();
		assertContains(errorMessage, "IO exception");
	}

	@Test
	public void testEndOfStream4() {
		String errorMessage = assertThrows(IOException.class, () -> new Bitser().deserialize(
				LargerStruct.class, new BitInputStream(new ByteArrayInputStream(new byte[0])), Bitser.BACKWARD_COMPATIBLE
		)).getMessage();
		assertContains(errorMessage, "End of stream reached");
	}
}
