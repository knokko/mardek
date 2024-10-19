package mardek.input

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicIntegerArray

class InputManager {

	private val eventQueue = LinkedBlockingQueue<InputKeyEvent>()
	private val pressedKeys = AtomicIntegerArray(InputKey.entries.size)

	fun postEvent(event: InputKeyEvent) {
		pressedKeys.set(event.key.ordinal, if (event.didRelease) 0 else 1)
		eventQueue.add(event)
	}

	fun consumeEvent() = eventQueue.poll()

	fun isPressed(key: InputKey) = pressedKeys.get(key.ordinal) == 1
}
