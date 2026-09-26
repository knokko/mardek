package com.github.knokko.bitser.kwik;

import tech.kwik.core.QuicConnection;
import tech.kwik.core.QuicStream;
import tech.kwik.core.server.ServerConnectionConfig;
import tech.kwik.core.server.ServerConnector;

public interface BikServerProtocol {

	void configureConfig(ServerConnectionConfig.Builder builder);

	void configureConnector(ServerConnector.Builder builder);

	boolean runHandshake(QuicConnection clientConnection, QuicStream mainStream);

	void waitUntilServerShouldStop();
}
