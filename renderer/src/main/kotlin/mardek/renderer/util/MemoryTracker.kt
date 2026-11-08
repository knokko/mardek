package mardek.renderer.util

import com.github.knokko.memory.MemorySnapshot
import java.lang.Thread.sleep

object MemoryTracker {

	@Volatile
	var lastSnapshot = MemorySnapshot.take()!!
		private set

	init {
		val memoryThread = Thread {
			while (true) {
				lastSnapshot = MemorySnapshot.take()
				sleep(1000)
			}
		}
		memoryThread.isDaemon = true
		memoryThread.start()
	}
}
