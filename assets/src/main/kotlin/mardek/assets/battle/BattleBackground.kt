package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.sprite.BcSprite
import java.util.*

@BitStruct(backwardCompatible = true)
class BattleBackground(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val sprite: BcSprite,
) {

	@BitField(id = 2)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this("", BcSprite())
}
