package mardek.content.audio

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite

/**
 * Represents a music category (e.g. Area Music) in the Music Player.
 */
@BitStruct(backwardCompatible = true)
class MusicCategory(

	/**
	 * The display name of this category, e.g. "Area Music" or "Battle Music".
	 */
	@BitField(id = 0)
	val displayName: String,

	/**
	 * The icon of this category, which is typically a colored music note.
	 */
	@BitField(id = 1)
	val icon: BcSprite,
) {

	internal constructor() : this("", BcSprite())
}
