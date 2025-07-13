package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.content.sprite.BcSprite

@BitStruct(backwardCompatible = true)
class TitleScreenContent(

	@BitField(id = 0)
	val background: BcSprite,

	@BitField(id = 1)
	val title: BcSprite,
) {

	@Suppress("unused")
	private constructor() : this(BcSprite(), BcSprite())
}
