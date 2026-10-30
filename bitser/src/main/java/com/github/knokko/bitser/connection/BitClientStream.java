package com.github.knokko.bitser.connection;

import com.github.knokko.bitser.io.BitInputStream;
import com.github.knokko.bitser.io.BitOutputStream;

public interface BitClientStream {

	void start(InputReader processInput);

	void send(OutputWriter sendFrame);

	void close();

	interface Factory {

		BitClientStream createStream(long controllerID, int viewID);
	}

	interface InputReader {

		void read(BitInputStream fromServer) throws Throwable;
	}

	interface OutputWriter {

		void write(BitOutputStream toServer) throws Throwable;
	}
}
