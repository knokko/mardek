package mardek.audio

import java.lang.Float.parseFloat
import java.util.Scanner

internal object AudioLooping {

	internal fun parse(): Map<String, Float> {
		val mapping = mutableMapOf<String, Float>()
		val scanner = Scanner(AudioManager::class.java.getResourceAsStream("loops.txt")!!)
		while (scanner.hasNextLine()) {
			val pairs = scanner.nextLine().split(":")
			if (pairs.size == 2 && pairs[1].endsWith(',')) {
				mapping[pairs[0]] = parseFloat(pairs[1].substring(0, pairs[1].length - 1))
			}
		}
		scanner.close()
		return mapping
	}
}
