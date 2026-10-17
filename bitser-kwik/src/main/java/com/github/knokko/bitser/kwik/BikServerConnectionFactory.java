package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.BitServer;
import tech.kwik.core.QuicConnection;
import tech.kwik.core.server.ApplicationProtocolConnection;
import tech.kwik.core.server.ApplicationProtocolConnectionFactory;

class BikServerConnectionFactory implements ApplicationProtocolConnectionFactory {

	private final BikServerProtocol bikProtocol;
	private final BitServer.ControllerContext controllerContext;

	BikServerConnectionFactory(BikServerProtocol bikProtocol, BitServer.ControllerContext controllerContext) {
		this.bikProtocol = bikProtocol;
		this.controllerContext = controllerContext;
	}

	@Override
	public ApplicationProtocolConnection createConnection(String protocol, QuicConnection quicConnection) {
		var nextConnection = new BikServerConnection(bikProtocol, controllerContext, quicConnection);
		nextConnection.start();
		return nextConnection;
	}

	@Override
	public int maxConcurrentPeerInitiatedUnidirectionalStreams() {
		return 0;
	}

	@Override
	public int maxConcurrentPeerInitiatedBidirectionalStreams() {
		return 0;
	}
}
