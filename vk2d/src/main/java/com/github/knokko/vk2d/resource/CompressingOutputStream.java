package com.github.knokko.vk2d.resource;

import org.lwjgl.util.zstd.ZSTDInBuffer;
import org.lwjgl.util.zstd.ZSTDOutBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.zstd.Zstd.*;

public class CompressingOutputStream extends OutputStream {

	private static final int BUFFER_SIZE = 1024 * 1024;

	private final WritableByteChannel channel;
	private final long zStream = ZSTD_createCCtx();
	private final ZSTDInBuffer zInput = ZSTDInBuffer.calloc();
	private final ZSTDOutBuffer zOutput = ZSTDOutBuffer.calloc();
	private final ByteBuffer inputBuffer = memCalloc(BUFFER_SIZE);
	private final ByteBuffer outputBuffer = memCalloc(BUFFER_SIZE);

	public CompressingOutputStream(WritableByteChannel channel, int compressionLevel) {
		this.channel = channel;
		ZSTD_CCtx_setParameter(zStream, ZSTD_c_compressionLevel, compressionLevel);
		zInput.set(inputBuffer, 0);
		zOutput.set(outputBuffer, 0);
	}

	public CompressingOutputStream(Path outputPath, int compressionLevel) throws IOException {
		this(FileChannel.open(
				outputPath,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE
		), compressionLevel);
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b });
	}

	@Override
	public void write(byte[] source, int sourceOffset, int sourceAmount) throws IOException {

		int sourceEndIndex = sourceOffset + sourceAmount;
		int sourcePosition = sourceOffset;
		while (sourcePosition < sourceEndIndex) {
			int batchSize = min(sourceEndIndex - sourcePosition, BUFFER_SIZE);
			inputBuffer.put(0, source, sourcePosition, batchSize);
			sourcePosition += batchSize;

			ZSTDInBuffer.nsize(zInput.address(), batchSize);
			zInput.pos(0);

			while (zInput.pos() < batchSize) {
				zOutput.pos(0);
				long compressionResult = ZSTD_compressStream2(zStream, zOutput, zInput, ZSTD_e_continue);
				if (ZSTD_isError(compressionResult)) throw new IOException(
						"Compression failed with " + compressionResult + " ( " + ZSTD_getErrorName(compressionResult) + ")"
				);

				outputBuffer.position(0);
				outputBuffer.limit(toIntExact(zOutput.pos()));
				while (outputBuffer.position() < zOutput.pos()) {
					channel.write(outputBuffer);
				}
			}
		}
	}

	private void flushOrClose(int op) throws IOException {
		while (true) {
			zOutput.pos(0);
			zInput.pos(0);
			ZSTDInBuffer.nsize(zInput.address(), 0L);
			long compressionResult = ZSTD_compressStream2(zStream, zOutput, zInput, op);
			if (ZSTD_isError(compressionResult)) throw new IOException(
					"Compression failed with " + compressionResult + " ( " + ZSTD_getErrorName(compressionResult) + ")"
			);

			outputBuffer.position(0);
			outputBuffer.limit(toIntExact(zOutput.pos()));
			while (outputBuffer.position() < zOutput.pos()) {
				channel.write(outputBuffer);
			}
			outputBuffer.position(0);
			if (compressionResult == 0L) return;
		}
	}

	@Override
	public void flush() throws IOException {
		flushOrClose(ZSTD_e_flush);
		super.flush();
	}

	@Override
	public void close() throws IOException {
		flushOrClose(ZSTD_e_end);
		super.close();

		channel.close();
		memFree(outputBuffer);
		memFree(inputBuffer);
		zOutput.free();
		zInput.free();
		ZSTD_freeCCtx(zStream);
	}
}
