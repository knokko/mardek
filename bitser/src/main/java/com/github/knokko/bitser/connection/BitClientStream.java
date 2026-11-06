package com.github.knokko.bitser.connection;

public interface BitClientStream {

	void start(ConnectionHelper.BitReader processInput);

	void send(ConnectionHelper.BitWriter sendFrame);

	void close();

	interface Factory {

		BitClientStream createStream(long controllerID, int viewID);

		// TODO BITSER Add method to register callback
	}
}
