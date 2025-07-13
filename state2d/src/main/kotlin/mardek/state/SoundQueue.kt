package mardek.state

import mardek.content.audio.SoundEffect
import java.util.concurrent.ArrayBlockingQueue

class SoundQueue {

	private val internal = ArrayBlockingQueue<SoundEffect>(5)

	fun insert(sound: SoundEffect) {
		if (!internal.offer(sound)) println("WARNING: too many sounds! skipped $sound")
	}

	fun take(): SoundEffect? = internal.poll()
}
