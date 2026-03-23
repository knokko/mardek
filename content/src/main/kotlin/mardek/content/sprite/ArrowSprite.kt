package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * The sprite of a 'transition arrow' at an area transition (e.g. the exit/entrance of Heroes' Den or Goznor). The
 * opacity of these arrows 'oscillate'.
 *
 * Vanilla MARDEK has exactly 4 arrow sprites: Up, Down, Left, and Right
 */
@BitStruct(backwardCompatible = true)
class ArrowSprite(

	/**
	 * The name of the sprite, which is only used for importing and editing
	 */
	@BitField(id = 0)
	val flashName: String,

	/**
	 * The actual sprite of the arrow
	 */
	@BitField(id = 1)
	val sprite: KimSprite,
) {

	@Suppress("unused")
	private constructor() : this("", KimSprite())

	override fun toString() = "ArrowSprite($flashName)"
}
