package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.BitServer;
import com.github.knokko.bitser.connection.StructConnectionView;
import tech.kwik.core.server.ServerConnectionConfig;
import tech.kwik.core.server.ServerConnector;

import java.io.IOException;
import java.security.cert.CertificateException;

public class BikServer {

	public static <T> void run(
			BikServerProtocol bikProtocol, String protocolName,
			T rootStruct, StructConnectionView rootView
	) throws CertificateException, IOException {
		var configBuilder = ServerConnectionConfig.builder()
				.maxTotalPeerInitiatedBidirectionalStreams(0)
				.maxTotalPeerInitiatedUnidirectionalStreams(0)
				.maxOpenPeerInitiatedBidirectionalStreams(0)
				.maxOpenPeerInitiatedUnidirectionalStreams(0)
				.maxUnidirectionalStreamBufferSize(0L);

		bikProtocol.configureConfig(configBuilder);
		var config = configBuilder.build();

		var connectorBuilder = ServerConnector.builder().withConfiguration(config);

		bikProtocol.configureConnector(connectorBuilder);

		try (var connector = connectorBuilder.build()) {
			var controllerMapping = new BitServer.ControllerMapping();
			var rootController = new BitServer.StructController<>(controllerMapping, rootStruct, rootView);
			controllerMapping.addWithChildren(rootController);

			connector.registerApplicationProtocol(
					protocolName, new BikServerConnectionFactory(bikProtocol, controllerMapping)
			);
			connector.start();
			bikProtocol.waitUntilServerShouldStop();
		}
	}
}
