package mardek.state

import java.util.concurrent.ArrayBlockingQueue

class SoundQueue {

	private val internal = ArrayBlockingQueue<String>(1)

	fun insert(sound: String) {
		internal.add(sound)
	}

	fun take() = internal.poll()
}
