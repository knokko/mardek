package mardek.assets.sprite

import java.awt.image.BufferedImage

/**
 * A 'tower' of 1 or more sprites, where the first sprite is rendered at the top, and all subsequent sprites are
 * rendered 16 'pixels' below the previous sprite.
 *
 * Most tiles have a sprite tower with a height of 1, which is just 1 sprite. But, some larger tiles like trees have a
 * height of 2. In this case, the last sprite of the tower contains the root/start of the tree, and the first
 * sprite contains the leaves.
 */
class SpriteTower(private val sprites: List<BufferedImage>) {

	init {
		if (sprites.isEmpty()) throw IllegalArgumentException("No sprites")
	}

	val height: Int
		get() = sprites.size

	operator fun get(index: Int) = sprites[index]

	fun last() = get(height - 1)
}
