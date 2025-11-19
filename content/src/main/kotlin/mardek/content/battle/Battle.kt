package mardek.content.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField

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
	constructor() : this(
		arrayOf(null, null, null, null),
		PartyLayout(), "", BattleBackground()
	)
}

/**
 * Represents an `Enemy` that the player may need to fight in a `Battle`. It is basically a tuple `(Monster, level)`.
 *
 * Note that this class is only relevant *before* a battle starts: it does **not** contain any state like current health
 * because enemies always start with full health & mana.
 */
@BitStruct(backwardCompatible = true)
class Enemy(

	/**
	 * The monster that they player would have to fight. This contains all base stats like maximum health, maximum mana,
	 * etc...
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "monsters")
	val monster: Monster,

	/**
	 * The level of the monster. This typically influences the strength & health of the monster.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val level: Int,

	/**
	 * When `overrideDisplayName == null`, the monster name rendered above the health bar will be `monster.displayName`.
	 * But, when `overrideDisplayName != null`, the rendered name will be `overrideDisplayName`.
	 */
	@BitField(id = 2, optional = true)
	val overrideDisplayName: String? = null,
) {
	@Suppress("unused")
	private constructor() : this(Monster(), 0)
}
