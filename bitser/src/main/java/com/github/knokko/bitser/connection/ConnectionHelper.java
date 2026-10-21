package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class ConnectionHelper {

	@FunctionalInterface
	interface BitWriter {

		void write(BitOutputStream output) throws Throwable;
	}

	static byte[] capture(BitWriter writer) throws Throwable {
		var rememberBytes = new ByteArrayOutputStream();
		var bitOutput = new BitOutputStream(rememberBytes);
		writer.write(bitOutput);
		bitOutput.finish();
		return rememberBytes.toByteArray();
	}
}
