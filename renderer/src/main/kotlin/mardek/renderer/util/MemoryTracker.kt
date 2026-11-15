package mardek.renderer.util

import com.github.knokko.memory.MemorySnapshot
import java.lang.Thread.sleep

object MemoryTracker {

	@Volatile
	var lastSnapshot = MemorySnapshot.take()!!
		private set

	init {
		val memoryThread = Thread {
			var hasSeenError = false
			while (true) {
				lastSnapshot = MemorySnapshot.take()
				if (!hasSeenError && lastSnapshot.processMemoryFailedException != null) {
					lastSnapshot.processMemoryFailedException.printStackTrace()
					hasSeenError = true
				}
				sleep(1000)
			}
		}
		memoryThread.name = "MemoryTracker"
		memoryThread.isDaemon = true
		memoryThread.start()
	}
}
