package com.github.knokko.bitser.kwik;

import tech.kwik.core.QuicClientConnection;
import tech.kwik.core.QuicStream;

import java.io.IOException;

public interface BikClientProtocol {

	void configureConnection(QuicClientConnection.Builder builder);

	void runHandshake(QuicStream stream) throws IOException;
}
