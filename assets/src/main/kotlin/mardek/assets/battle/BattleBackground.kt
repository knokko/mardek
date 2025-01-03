package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.assets.sprite.BcSprite

@BitStruct(backwardCompatible = false)
class BattleBackground(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val sprite: BcSprite,
) {

	internal constructor() : this("", BcSprite())
}
