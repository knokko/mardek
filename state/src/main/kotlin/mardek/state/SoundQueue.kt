package mardek.state

import mardek.content.audio.SoundEffect
import java.util.concurrent.ArrayBlockingQueue

/**
 * This class facilitates the communication from the game state to the audio player: the game state [insert]s sounds
 * that it wants to be played, and the audio player will periodically player all sounds that it can [take] from this
 * queue.
 *
 * This class also facilitates unit testing: the unit test can use the [take] method to verify that the game state
 * inserted the right sounds into the queue.
 */
class SoundQueue {

	private val internal = ArrayBlockingQueue<SoundEffect>(5)

	/**
	 * Inserts a sound into the queue.
	 *
	 * If the queue is full, a warning will be printed instead.
	 */
	fun insert(sound: SoundEffect) {
		if (!internal.offer(sound)) println("WARNING: too many sounds! skipped $sound")
	}

	/**
	 * Removes and returns the next sound from this queue.
	 *
	 * When this queue is empty, this method returns `null` instead.
	 */
	fun take(): SoundEffect? = internal.poll()
}
