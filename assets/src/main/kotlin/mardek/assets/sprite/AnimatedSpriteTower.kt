package mardek.assets.sprite

/**
 * An animated 'tower' of 1 or more sprites. Each frame has its own sprite tower.
 */
class AnimatedSpriteTower(private val frames: List<SpriteTower>) {

	init {
		if (frames.isEmpty()) throw IllegalArgumentException("No frames")
		for (frame in frames) {
			if (frame.height != frames[0].height) throw IllegalArgumentException("All frames must have the same height")
		}
	}

	val numFrames: Int
		get() = frames.size

	val height: Int
		get() = frames[0].height

	operator fun get(index: Int) = frames[index]
}
