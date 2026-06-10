package mardek.audio

import java.lang.Thread.sleep

fun main() {
	val manager = AudioManager()
	val shockInput = AudioManager::class.java.getResourceAsStream("shock.ogg")!!
	val shockBytes = shockInput.readAllBytes()
	shockInput.close()
	val shock = manager.add(null, shockBytes)

	manager.playSound(shock)
	sleep(1000)
	manager.destroy()
}
