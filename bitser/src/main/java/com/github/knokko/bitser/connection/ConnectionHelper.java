package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

import java.io.ByteArrayOutputStream;

public class ConnectionHelper {

	@FunctionalInterface
	public interface BitWriter {

		void write(BitOutputStream output) throws Throwable;
	}

	@FunctionalInterface
	public interface BitReader {

		void read(BitInputStream input) throws Throwable;
	}

	static byte[] capture(BitWriter writer) throws Throwable {
		var rememberBytes = new ByteArrayOutputStream();
		var bitOutput = new BitOutputStream(rememberBytes);
		writer.write(bitOutput);
		bitOutput.finish();
		return rememberBytes.toByteArray();
	}
}
