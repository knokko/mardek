package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.battle.BattleBackground
import mardek.assets.battle.Monster
import mardek.assets.battle.PartyLayout

@BitStruct(backwardCompatible = false)
class Battle(

	@BitField(ordering = 0)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val enemies: Array<Enemy?>,

	@BitField(ordering = 1)
	@ReferenceField(stable = true, label = "enemy party layouts")
	val enemyPositions: PartyLayout,

	@BitField(ordering = 2)
	val music: String,

	@BitField(ordering = 3)
	@ReferenceField(stable = true, label = "battle backgrounds")
	val background: BattleBackground,
) {
	internal constructor() : this(emptyArray(), PartyLayout(), "", BattleBackground())
}

@BitStruct(backwardCompatible = false)
class Enemy(
	@BitField(ordering = 0)
	val monster: Monster,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val level: Int
) {
	@Suppress("unused")
	private constructor() : this(Monster(), 0)
}
