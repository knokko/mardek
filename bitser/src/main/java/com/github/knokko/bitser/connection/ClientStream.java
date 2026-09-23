package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

public interface ClientStream {

	void start(InputReader processInput);

	void send(OutputWriter sendFrame);

	void close();

	interface Factory {

		ClientStream createStream(long controllerID);
	}

	interface InputReader {

		void read(BitInputStream fromServer) throws Throwable;
	}

	interface OutputWriter {

		void write(BitOutputStream toServer) throws Throwable;
	}
}
