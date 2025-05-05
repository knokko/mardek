package mardek.state

import java.util.concurrent.ArrayBlockingQueue

class SoundQueue {

	private val internal = ArrayBlockingQueue<String>(5)

	fun insert(sound: String) {
		internal.offer(sound)
	}

	fun take(): String? = internal.poll()
}
