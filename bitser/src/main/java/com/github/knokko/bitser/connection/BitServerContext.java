package com.github.knokko.bitser.connection;

import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;

public record BitServerContext(
		Object rootStruct,
		BitServerControllerMapping controllerMapping,
		ReadWriteLock globalLock,
		Queue<GlobalRunnable> globalQueue
) {

	@FunctionalInterface
	public interface GlobalRunnable {

		void run() throws Throwable;
	}
}
