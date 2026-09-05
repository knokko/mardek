package com.github.knokko.bitser.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class ConnectionHelper {

	static final byte[] STOP_SIGN = new byte[0];

	static void sendPacket(byte[] packet, DataOutputStream output) throws IOException {
		if (packet.length > 254) {
			output.write(255);
			output.writeInt(packet.length);
		} else output.write(packet.length);
		output.write(packet);
		output.flush();
	}

	static byte[] readPacket(DataInputStream input) throws IOException {
		int size = input.readUnsignedByte();
		if (size == 255) size = input.readInt();
		byte[] packet = new byte[size];
		input.readFully(packet);
		return packet;
	}
}
