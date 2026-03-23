package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * A group of strongly-related sprites. When rendered, the renderer will choose **one of the sprites** to render.
 * The choice of the sprite depends on the direction of the object/character that is being rendered.
 *
 * The renderer should 'know' at which index the sprite of each direction is stored..
 * For instance, for walking area characters, the walking sprites for each
 * `Direction` are at indices `2 * direction.ordinal` and `1 + 2 * direction.ordinal`.
 */
@BitStruct(backwardCompatible = true)
class DirectionalSprites(

	/**
	 * The name of the sprites, e.g. 'mardek_hero' for the walking sprites of Hero Mardek. This name is useful during
	 * editing and importing, but doesn't serve any in-game purpose.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The sprites of this group/collection. This renderer should know which sprite is at which index.
	 */
	@BitField(id = 1)
	val sprites: Array<KimSprite>
) {

	override fun toString() = name

	constructor() : this("", emptyArray())
}
