package com.github.knokko.vk2d.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestCompressingOutputStream {

	private void runTest(byte[] payload) throws IOException {
		var testFile = Files.createTempFile("", "");
		testFile.toFile().deleteOnExit();

		var output = new CompressingOutputStream(testFile, 5);
		output.write(payload);
		output.flush();
		output.close();

		var input = new DecompressingInputStream(testFile);
		assertArrayEquals(payload, input.readAllBytes());
		input.close();
		assertTrue(testFile.toFile().delete());
	}

	@Test
	public void testEmptyFile() throws IOException {
		runTest(new byte[0]);
	}

	@Test
	public void testSingleByteFile() throws IOException {
		runTest(new byte[] { 12 });
	}

	@Test
	public void testSmallFile() throws IOException {
		var bytes = new byte[100];
		for (byte index = 0; index < bytes.length; index++) bytes[index] = index;
		runTest(bytes);
	}

	@Test
	public void testLargerFile() throws IOException {
		var bytes = new byte[50 * 999 * 888];
		for (int index = 0; index < bytes.length; index++) bytes[index] = (byte) index;
		runTest(bytes);
	}

	@Test
	public void testMediumRandomFile() throws IOException {
		var bytes = new byte[64000];
		new Random(1234).nextBytes(bytes);
		runTest(bytes);
	}

	@Test
	public void testLargerRandomFile() throws IOException {
		var bytes = new byte[50 * 999 * 888];
		new Random(1234).nextBytes(bytes);
		runTest(bytes);
	}
}
