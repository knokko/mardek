package com.github.knokko.vk2d.resource;

import org.lwjgl.util.zstd.ZSTDInBuffer;
import org.lwjgl.util.zstd.ZSTDOutBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.lang.Math.toIntExact;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.zstd.Zstd.*;

public class DecompressingInputStream extends InputStream {

	private static final int BUFFER_SIZE = 1024 * 1024;

	private final ReadableByteChannel channel;
	private final long zStream = ZSTD_createDStream();
	private final ZSTDInBuffer zInput = ZSTDInBuffer.calloc();
	private final ZSTDOutBuffer zOutput = ZSTDOutBuffer.calloc();
	private final ByteBuffer inputBuffer = memCalloc(BUFFER_SIZE);
	private final ByteBuffer outputBuffer = memCalloc(BUFFER_SIZE);

	private int consumedOutputUntil = 0;
	private int filledInputUntil = 0;


	public DecompressingInputStream(ReadableByteChannel channel) {
		this.channel = channel;
		zInput.set(inputBuffer, 0);
		zOutput.set(outputBuffer, 0);
	}

	public DecompressingInputStream(Path inputFile) throws IOException {
		this(FileChannel.open(inputFile, StandardOpenOption.READ));
	}

	@Override
	public int read() throws IOException {
		var dummyArray = new byte[1];
		int readResult = read(dummyArray, 0, 1);
		return readResult == -1 ? -1 : dummyArray[0] & 0xFF;
	}

	@Override
	public int read(byte[] destination, int destinationOffset, int destinationAmount) throws IOException {
		if (destinationAmount == 0) return 0;

		if (zOutput.pos() > 0L) {
			int numOutputBytes = toIntExact(zOutput.pos()) - consumedOutputUntil;
			if (numOutputBytes <= destinationAmount) {
				outputBuffer.get(consumedOutputUntil, destination, destinationOffset, numOutputBytes);
				zOutput.pos(0L);
				consumedOutputUntil = 0;
				return numOutputBytes;
			} else {
				outputBuffer.get(consumedOutputUntil, destination, destinationOffset, destinationAmount);
				consumedOutputUntil += destinationAmount;
				return destinationAmount;
			}
		}

		if (filledInputUntil == zInput.pos()) {
			inputBuffer.position(0);
			int numReadBytes = channel.read(inputBuffer);
			if (numReadBytes == -1) return -1;
			filledInputUntil = numReadBytes;
			zInput.pos(0);
		}

		long decompressResult = ZSTD_decompressStream(zStream, zOutput, zInput);
		if (ZSTD_isError(decompressResult)) throw new IOException(
				"Decompression failed with " + decompressResult + "(" + ZSTD_getErrorName(decompressResult) + ")"
		);

		int numOutputBytes = toIntExact(zOutput.pos());
		if (numOutputBytes == 0) {
			if (decompressResult == 0L) return -1;
			else throw new IOException("Unexpected/weird end of stream");
		}
		if (numOutputBytes <= destinationAmount) {
			outputBuffer.get(0, destination, destinationOffset, numOutputBytes);
			zOutput.pos(0L);
			return numOutputBytes;
		} else {
			outputBuffer.get(0, destination, destinationOffset, destinationAmount);
			consumedOutputUntil += destinationAmount;
			return destinationAmount;
		}
	}

	@Override
	public void close() throws IOException {
		super.close();

		memFree(outputBuffer);
		memFree(inputBuffer);
		zOutput.free();
		zInput.free();
		ZSTD_freeDStream(zStream);
		channel.close();
	}
}
