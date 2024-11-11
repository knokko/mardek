package mardek.assets

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.assets.area.sprites.DirectionalSpritesheet

@BitStruct(backwardCompatible = false)
class PlayableCharacter(

	@BitField(ordering = 0)
	val areaSheet: DirectionalSpritesheet
) {

	@Suppress("unused")
	private constructor() : this(DirectionalSpritesheet())
}
