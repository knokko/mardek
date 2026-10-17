package com.github.knokko.bitser.kwik;

import com.github.knokko.bitser.connection.BitServer;
import com.github.knokko.bitser.connection.StructConnectionView;
import tech.kwik.core.server.ServerConnectionConfig;
import tech.kwik.core.server.ServerConnector;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BikServer {

	private static final BitServer.GlobalRunnable SERVER_STOPPED = () -> {};

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

		var globalLock = new ReentrantReadWriteLock();
		var globalQueue = createGlobalQueueWithDrainingThread(globalLock);

		try (var connector = connectorBuilder.build()) {
			var controllerMapping = new BitServer.ControllerMapping();
			var controllerContext = new BitServer.ControllerContext(
					rootStruct, controllerMapping, globalLock, globalQueue
			);
			var rootController = new BitServer.StructController<>(controllerContext, rootStruct, rootView);
			controllerMapping.addWithChildren(rootController);

			connector.registerApplicationProtocol(
					protocolName, new BikServerConnectionFactory(bikProtocol, controllerContext)
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

	private static BlockingQueue<BitServer.GlobalRunnable> createGlobalQueueWithDrainingThread(ReadWriteLock lock) {
		// TODO BITSER Move this logic to BitServer?
		var queue = new ArrayBlockingQueue<BitServer.GlobalRunnable>(100);

		var thread = new Thread(() -> {
			while (true) {
				BitServer.GlobalRunnable nextTask;
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
