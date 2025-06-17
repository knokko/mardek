package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.battle.BattleBackground
import mardek.content.battle.Monster
import mardek.content.battle.PartyLayout

@BitStruct(backwardCompatible = true)
class Battle(

	@BitField(id = 0)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val startingEnemies: Array<Enemy?>,

	@BitField(id = 1)
	@ReferenceField(stable = true, label = "enemy party layouts")
	val enemyLayout: PartyLayout,

	@BitField(id = 2)
	val music: String,

	@BitField(id = 3)
	@ReferenceField(stable = true, label = "battle backgrounds")
	val background: BattleBackground,
) {
	internal constructor() : this(arrayOf(null, null, null, null), PartyLayout(), "", BattleBackground())
}

@BitStruct(backwardCompatible = true)
class Enemy(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "monsters")
	val monster: Monster,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val level: Int
) {
	@Suppress("unused")
	private constructor() : this(Monster(), 0)
}
