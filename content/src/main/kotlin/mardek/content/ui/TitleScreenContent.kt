package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite

/**
 * This is a minified version of [mardek.content.Content] that is needed by the title screen. This content must be
 * loaded right when the game launches, whereas the real `Content` is only needed after loading a save (or starting
 * a new save).
 */
@BitStruct(backwardCompatible = true)
class TitleScreenContent(

	/**
	 * The title screen background image
	 */
	@BitField(id = 0)
	val background: BcSprite,

	/**
	 * The basic font used by the title screen
	 */
	@BitField(id = 1)
	val basicFont: Font,

	/**
	 * The fat font used by the title screen
	 */
	@BitField(id = 2)
	val fatFont: Font,

	/**
	 * The full-uppercase font used by the title screen
	 */
	@BitField(id = 3)
	val largeFont: Font,
) {

	@Suppress("unused")
	private constructor() : this(BcSprite(), Font(), Font(), Font())
}
