package com.github.knokko.bitser.test.io;

import com.github.knokko.bitser.IntegerBitser;
import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegerBenchmark {

	public static void main(String[] args) throws IOException {
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(100_000_000);
		DataOutputStream dataOutput = new DataOutputStream(byteOutput);
		BitOutputStream bitOutput = new BitOutputStream(byteOutput);

		int numRepetitions = 10_000;
		int minValue = 0;
		int maxValue = 20_000;
		long startTime = System.nanoTime();
		for (int repetition = 0; repetition < numRepetitions; repetition++) {
			for (int number = minValue; number < maxValue; number++) {
				IntegerBitser.encodeUniformInteger(number, 0L, 123456L, bitOutput);
				//bitOutput.write(true);
				//dataOutput.writeLong(number);
				//IntegerBitser.encodeFullLong(number, bitOutput);
				//IntegerBitser.encodeVariableIntegerUsingTerminatorBits(number, 0L, 123456L, bitOutput);
				//dataOutput.writeInt(number);
//				byteOutput.write((byte) (number >> 24));
//				byteOutput.write((byte) (number >> 16));
//				byteOutput.write((byte) (number >> 8));
//				byteOutput.write((byte) number);
//				bitOutput.write(number, 4);
//				bitOutput.write(number >> 4, 4);
//				bitOutput.write(number >> 8, 4);
//				bitOutput.write(number >> 12, 4);
//				bitOutput.write(number >> 16, 4);
//				bitOutput.write(number >> 20, 4);
//				bitOutput.write(number >> 24, 4);
//				bitOutput.write(number >> 28, 4);
				//for (int shift = 0; shift < 32; shift += 4) bitOutput.write(number >> shift, 4);
				//IntegerBitser.encodeDigitInteger(number, 0L, 123456L, 4, bitOutput);
			}
		}

		long midTime = System.nanoTime();
		System.out.println("Encoding took " + (midTime - startTime) / 1000_000L + "ms for " + byteOutput.toByteArray().length / 1000 + "kB");

		bitOutput.finish();
		BitInputStream bitInput = new BitInputStream(new ByteArrayInputStream(byteOutput.toByteArray()));
		DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(byteOutput.toByteArray()));
		midTime = System.nanoTime();
		for (int repetition = 0; repetition < numRepetitions; repetition++) {
			for (int number = minValue; number < maxValue; number++) {
				//assertTrue(bitInput.read());
				//assertEquals(number, IntegerBitser.decodeDigitInteger(0L, 123456L, 4, bitInput));
				//assertEquals(number, dataInput.readInt());
				//assertEquals(number, IntegerBitser.decodeFullLong(bitInput));
				//assertEquals(number, dataInput.readLong());
				assertEquals(number, IntegerBitser.decodeUniformInteger(0L, 123456L, bitInput));
			}
		}

		long endTime = System.nanoTime();
		System.out.println("Decoding took " + (endTime - midTime) / 1000_000L + "ms");
	}
}
