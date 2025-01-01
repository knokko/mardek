package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.sprite.BcSprite
import java.util.*

@BitStruct(backwardCompatible = false)
class BattleBackground(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val sprite: BcSprite,
) {

	@BitField(ordering = 2)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this("", BcSprite())
}
