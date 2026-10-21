package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.BitServerContext;
import tech.kwik.core.QuicConnection;
import tech.kwik.core.server.ApplicationProtocolConnection;
import tech.kwik.core.server.ApplicationProtocolConnectionFactory;

class BikServerConnectionFactory implements ApplicationProtocolConnectionFactory {

	private final BikServerProtocol bikProtocol;
	private final BitServerContext context;

	BikServerConnectionFactory(BikServerProtocol bikProtocol, BitServerContext context) {
		this.bikProtocol = bikProtocol;
		this.context = context;
	}

	@Override
	public ApplicationProtocolConnection createConnection(String protocol, QuicConnection quicConnection) {
		var nextConnection = new BikServerConnection(bikProtocol, context, quicConnection);
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
