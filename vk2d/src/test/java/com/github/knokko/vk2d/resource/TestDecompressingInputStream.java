package com.github.knokko.vk2d.resource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

import static java.lang.Math.toIntExact;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.zstd.Zstd.ZSTD_compress;
import static org.lwjgl.util.zstd.Zstd.ZSTD_compressBound;

public class TestDecompressingInputStream {

	private static byte[] compress(byte[] original) {
		var uncompressedBuffer = memCalloc(original.length);
		uncompressedBuffer.put(0, original);

		var compressedCapacity = ZSTD_compressBound(original.length);
		var compressedBuffer = memCalloc(toIntExact(compressedCapacity));
		var compressedSize = ZSTD_compress(compressedBuffer, uncompressedBuffer, 5);
		compressedBuffer.limit(toIntExact(compressedSize));

		var compressedBytes = new byte[toIntExact(compressedSize)];
		compressedBuffer.get(compressedBytes);

		memFree(uncompressedBuffer);
		memFree(compressedBuffer);

		return compressedBytes;
	}

	private void runTest(byte[] payload) throws IOException {
		var testFile = Files.createTempFile("", "");
		testFile.toFile().deleteOnExit();

		byte[] compressed = compress(payload);

		// Sanity check
		Files.write(testFile, compressed);

		var input = new DecompressingInputStream(testFile);
		var decompressed = input.readAllBytes();
		input.close();

		Files.deleteIfExists(testFile);
		assertArrayEquals(payload, decompressed);
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
