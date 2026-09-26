package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.BitServer;
import tech.kwik.core.QuicConnection;
import tech.kwik.core.QuicStream;
import tech.kwik.core.server.ApplicationProtocolConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class BikServerConnection implements ApplicationProtocolConnection {

	private final BikServerProtocol bikProtocol;
	private final BitServer.ControllerMapping controllers;
	private final QuicConnection clientConnection;

	BikServerConnection(
			BikServerProtocol bikProtocol,
			BitServer.ControllerMapping controllers,
			QuicConnection clientConnection
	) {
		this.bikProtocol = bikProtocol;
		this.controllers = controllers;
		this.clientConnection = clientConnection;
	}

	void start() {
		var thread = new Thread(this::run);
		thread.setDaemon(true);
		thread.start();
	}

	private void run() {
		try {
			var mainStream = clientConnection.createStream(true);

			if (!bikProtocol.runHandshake(clientConnection, mainStream)) {
				System.err.println("Closed connection because handshake failed");
				clientConnection.close();
				return;
			}

			var keepAliveThread = new Thread(() -> this.runKeepAlive(mainStream));
			keepAliveThread.setDaemon(true);
			keepAliveThread.start();

			var dataInput = new DataInputStream(mainStream.getInputStream());
			while (true) {
				int requestedStreamID = dataInput.readInt();
				long controllerID = dataInput.readLong();
				var nextStream = clientConnection.createStream(true);
				var dataOutput = new DataOutputStream(nextStream.getOutputStream());
				dataOutput.writeInt(requestedStreamID);
				dataOutput.flush();

				var controller = (BitServer.StructController<?>) controllers.getControllerById(controllerID);
				controller.addClient(
						nextStream.getOutputStream(),
						nextStream.getInputStream(),
						() -> nextStream.resetStream(0L)
				);
			}
		} catch (IOException disconnected) {
			System.err.println("BikServerConnection disconnected: " + disconnected.getMessage());
		} finally {
			clientConnection.close();
		}
	}

	private void runKeepAlive(QuicStream mainStream) {
		try {
			while (true) {
				mainStream.getOutputStream().write(123);
				mainStream.getOutputStream().flush();
				//noinspection BusyWait
				Thread.sleep(10_000L);
			}
		} catch (Throwable died) {
			System.err.println("BikServerConnection keep-alive died: " + died.getMessage());
		} finally {
			clientConnection.close();
		}
	}
}
