package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.BitClient;
import com.github.knokko.bitser.connection.StructConnectionView;
import tech.kwik.core.QuicClientConnection;
import tech.kwik.core.QuicConnection;
import tech.kwik.core.QuicStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BikClient {

	public static BitClient.Struct connect(
			URI uri, String protocol, StructConnectionView rootView, BikClientProtocol bikProtocol
	) throws IOException {
		var builder = QuicClientConnection.newBuilder()
				.uri(uri)
				.applicationProtocol(protocol)
				.defaultStreamReceiveBufferSize(2500L) // Bitser packets are normally very small
				.maxOpenPeerInitiatedUnidirectionalStreams(0); // Bitser streams are always bidirectional
		bikProtocol.configureConnection(builder);

		var connection = builder.build();

		var rootStructFuture = new CompletableFuture<BitClient.Struct>();

		BikClientStreamFactory[] streamFactory = { null };

		connection.setPeerInitiatedStreamCallback(stream -> {

			if (streamFactory[0] == null) {
				try {
					//noinspection ResultOfMethodCallIgnored
					stream.getInputStream().read(); // Skip the first (dummy) byte

					bikProtocol.runHandshake(stream);

					streamFactory[0] = new BikClientStreamFactory(new DataOutputStream(stream.getOutputStream()));

					var rootConnection = new BitClient.Struct(
							rootView, streamFactory[0].createStream(0), streamFactory[0]
					);
					rootStructFuture.complete(rootConnection);
				} catch (Throwable failed) {
					rootStructFuture.completeExceptionally(failed);
				}

				runKeepAliveThread(connection, stream);
			} else {
				streamFactory[0].addStream(stream);
			}
		});
		connection.connect();

		try {
			return rootStructFuture.get(10, TimeUnit.SECONDS);
		} catch (Throwable failed) {
			throw new IOException(failed);
		}
	}

	private static void runKeepAliveThread(QuicConnection connection, QuicStream stream) {
		var keepAliveThread = new Thread(() -> {
			try {
				while (true) {
					if (stream.getInputStream().read() != 123) { // TODO Make it client-to-server instead?
						System.err.println("Keep-alive stream sent unexpected byte");
						return;
					}
				}
			} catch (IOException lostConnection) {
				System.err.println("Keep-alive thread lost connection: " + lostConnection.getMessage());
			} finally {
				connection.close();
			}
		});
		keepAliveThread.setDaemon(true);
		keepAliveThread.start();
	}
}
