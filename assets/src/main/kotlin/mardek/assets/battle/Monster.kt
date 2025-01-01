package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import mardek.assets.animations.BattleModel

@BitStruct(backwardCompatible = false)
class Monster(

	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val model: BattleModel,
) {

	constructor() : this("", BattleModel())

	override fun toString() = name
}
