package com.github.knokko.bitser.connection;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public record BitServerContext(
		Object rootStruct,
		BitServerControllerMapping controllerMapping,
		BitServerOperationRegistry operationRegistry,
		ReentrantReadWriteLock globalLock,
		Queue<GlobalRunnable> globalQueue
) {

	@FunctionalInterface
	public interface GlobalRunnable {

		void run() throws Throwable;
	}
}
