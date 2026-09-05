package com.github.knokko.bitser.test.io;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class BitOutputBenchmark {

	public static void main(String[] args) throws IOException {
		benchmark(new BitOutputStream(new BufferedOutputStream(Files.newOutputStream(new File("benchmark.bin").toPath()))));
		benchmark(new BitOutputStream(new BufferedOutputStream(Base64.getEncoder().wrap(Files.newOutputStream(new File("benchmark.b64").toPath())))));
	}

	private static void benchmark(BitOutputStream output) throws IOException {
		writeSimpleData(output, 1_000_000);

		long startTime = System.nanoTime();
		writeData(output);
		long endTime = System.nanoTime();
		System.out.println("Took " + ((endTime - startTime) / 1000_000) + "ms for " + output.getClass().getSimpleName());
	}

	private static void writeData(BitOutputStream output) throws IOException {
		writeSimpleData(output, 100_000_000);
		writeSimpleData(output, 100_000_000);
		for (long value = 0; value < 1_000_000; value++) {
			IntegerBitser.encodeUniformInteger(value, 0, 1000_000_000_000L, output);
			IntegerBitser.encodeVariableIntegerUsingTerminatorBits(value, 0, 1000_000_000_000L, output);
		}
		output.finish();
	}

	private static void writeSimpleData(BitOutputStream output, int amount) throws IOException {
		for (int counter = 0; counter < amount; counter++) output.write(true);
		for (int counter = 0; counter < amount; counter++) output.write(false);
	}
}
