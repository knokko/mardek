package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

/**
 * A group of strongly-related sprites. When rendered, the renderer will choose **one of the sprites** to render.
 * The choice of the sprite typically depends on the state of the object/character that is being rendered, or just on
 * the current time.
 *
 * This class does *not* provide any information about the 'meaning' of each of its sprites; the renderer should
 * simply 'know' how to render each object. For instance, sprite 0 of a door is always the closed door sprite.
 */
@BitStruct(backwardCompatible = true)
class ObjectSprites(

	/**
	 * The name of these sprites, as imported from Flash.
	 *
	 * This name is useful while importing and editing, but doesn't serve an in-game purpose.
	 */
	@BitField(id = 0)
	var flashName: String,

	/**
	 * The frames/sprites of this group/collection. This renderer should know which sprite is at which index.
	 */
	@BitField(id = 1)
	val frames: Array<KimSprite>,
) {

	internal constructor() : this("", emptyArray())
}
