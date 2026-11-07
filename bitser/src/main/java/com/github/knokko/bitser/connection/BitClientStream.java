package com.github.knokko.bitser.connection;

public interface BitClientStream {

	Factory getFactory();

	void start(ConnectionHelper.BitReader processInput);

	void send(ConnectionHelper.BitWriter sendFrame);

	void close();

	interface Factory {

		BitClientStream createStream(long controllerID, int viewID);

		int addResponseCallback(ConnectionHelper.BitReader readResponse);
	}
}
