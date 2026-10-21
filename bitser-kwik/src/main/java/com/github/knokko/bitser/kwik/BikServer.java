package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.*;
import tech.kwik.core.server.ServerConnectionConfig;
import tech.kwik.core.server.ServerConnector;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BikServer {

	private static final BitServerContext.GlobalRunnable SERVER_STOPPED = () -> {};

	public static <T> void run(
			BikServerProtocol bikProtocol, String protocolName,
			T rootStruct, List<BitStructConnectionView> rootViews
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
					rootStruct, controllerMapping, globalLock, globalQueue
			);
			controllerMapping.addIfNotPresent(serverContext, List.of(rootStruct), rootViews);

			connector.registerApplicationProtocol(
					protocolName, new BikServerConnectionFactory(bikProtocol, serverContext)
			);
			connector.start();
			bikProtocol.waitUntilServerShouldStop();
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
