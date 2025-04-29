package mardek.input

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLongArray

class InputManager {

	private val eventQueue = LinkedBlockingQueue<Event>()
	private val pressedKeys = AtomicLongArray(InputKey.entries.size)

	fun postEvent(event: Event) {
		if (event is InputKeyEvent) pressedKeys.set(event.key.ordinal, if (event.didRelease) 0L else System.nanoTime())
		eventQueue.add(event)
	}

	fun consumeEvent(): Event? = eventQueue.poll()

	fun isPressed(key: InputKey) = pressedKeys.get(key.ordinal) != 0L

	fun mostRecentlyPressed(keys: Array<InputKey>): InputKey? {
		val lastPressedTimes = keys.map { pressedKeys.get(it.ordinal) }
		var lastPressedTime = Long.MIN_VALUE
		var lastPressed: InputKey? = null
		for ((index, key) in keys.withIndex()) {
			if (lastPressedTimes[index] != 0L && lastPressedTimes[index] > lastPressedTime) {
				lastPressed = key
				lastPressedTime = lastPressedTimes[index]
			}
		}
		return lastPressed
	}
}
