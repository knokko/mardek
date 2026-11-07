package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.*;
import com.github.knokko.bitser.io.BitOutputStream;
import tech.kwik.core.QuicConnection;
import tech.kwik.core.server.ServerConnectionConfig;
import tech.kwik.core.server.ServerConnector;

import java.io.DataOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BikServer {

	private static final BitServerContext.GlobalRunnable SERVER_STOPPED = () -> {};

	private static void respondToClient(
			Object connectionHandle, int responseID, ConnectionHelper.BitWriter writeResponse
	) {
		new Thread(() -> {
			var connection = (QuicConnection) connectionHandle;

			try {
				var stream = connection.createStream(false);
				var rawOutput = stream.getOutputStream();
				var dataOutput = new DataOutputStream(rawOutput);
				dataOutput.writeInt(responseID);
				var output = new BitOutputStream(rawOutput);
				writeResponse.write(output);
				output.flush();
				System.out.println("Flushed output to client");
			} catch (Throwable failed) {
				System.err.println("Failed to send response to client: " + failed.getMessage());
			}
		}).start();
	}

	public static <T> void run(
			BikServerProtocol bikProtocol, String protocolName,
			T rootStruct, List<BitStructConnectionView> rootViews, BitServerOperationRegistry operationRegistry
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

		var globalLock = new ReentrantReadWriteLock();
		var globalQueue = createGlobalQueueWithDrainingThread(globalLock);

		try (var connector = connectorBuilder.build()) {
			var controllerMapping = new BitServerControllerMapping();
			var serverContext = new BitServerContext(
					rootStruct, controllerMapping, operationRegistry,
					globalLock, globalQueue, BikServer::respondToClient
			);
			controllerMapping.addIfNotPresent(serverContext, List.of(rootStruct), rootViews, new ArrayList<>());

			connector.registerApplicationProtocol(
					protocolName, new BikServerConnectionFactory(bikProtocol, serverContext)
			);
			connector.start();
			bikProtocol.waitUntilServerShouldStop(serverContext);
		} finally {
			try {
				globalQueue.put(SERVER_STOPPED);
			} catch (InterruptedException e) {
				System.err.println("Interrupted during SERVER_STOPPED insertion");
			}
		}
	}

	private static BlockingQueue<BitServerContext.GlobalRunnable> createGlobalQueueWithDrainingThread(ReadWriteLock lock) {
		// TODO BITSER Move this logic to BitServer?
		var queue = new ArrayBlockingQueue<BitServerContext.GlobalRunnable>(100);

		var thread = new Thread(() -> {
			while (true) {
				BitServerContext.GlobalRunnable nextTask;
				try {
					nextTask = queue.take();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (nextTask == SERVER_STOPPED) return;

				lock.writeLock().lock();
				try {
					nextTask.run();
				} catch (Throwable taskFailed) {
					System.err.println("Global task failed:");
					//noinspection CallToPrintStackTrace
					taskFailed.printStackTrace();
				} finally {
					lock.writeLock().unlock();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();

		return queue;
	}
}
