package com.github.knokko.bitser.connection;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public record BitServerContext(
		Object rootStruct,
		BitServerControllerMapping controllerMapping,
		BitServerOperationRegistry operationRegistry,
		ReentrantReadWriteLock globalLock,
		Queue<GlobalRunnable> globalQueue,
		RespondToClient respondToClient
) {

	@FunctionalInterface
	public interface GlobalRunnable {

		void run() throws Throwable;
	}

	@FunctionalInterface
	public interface RespondToClient {

		void send(Object connectionHandle, int responseID, ConnectionHelper.BitWriter writeData);
	}
}
