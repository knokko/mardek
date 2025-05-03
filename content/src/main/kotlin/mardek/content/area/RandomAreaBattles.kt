package mardek.content.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.BITSER
import mardek.content.battle.BattleBackground
import mardek.content.battle.Monster
import mardek.content.battle.PartyLayout

@BitStruct(backwardCompatible = true)
class RandomAreaBattles(

	@BitField(id = 0)
	@NestedFieldSetting(path = "", optional = true)
	val ownEnemies: ArrayList<BattleEnemySelection>?,

	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "enemy selections")
	val sharedEnemies: SharedEnemySelections?,

	@BitField(id = 2, optional = true)
	val ownLevelRange: LevelRange?,

	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "level ranges")
	val sharedLevelRange: SharedLevelRange?,

	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val minSteps: Int,

	@BitField(id = 5)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int, // Percentage after taking 1 step?

	@BitField(id = 6)
	val defaultBackground: BattleBackground,

	@BitField(id = 7, optional = true)
	val specialBackground: BattleBackground?
) {

	@Suppress("unused")
	private constructor() : this(
		ArrayList(0), null, LevelRange(), null,
		0, 0, BattleBackground(), null
	)

	init {
		if ((ownEnemies == null) == (sharedEnemies == null)) {
			throw IllegalArgumentException("Exactly 1 of ownEnemies and monstersTableName must be null")
		}
		if ((ownLevelRange == null) == (sharedLevelRange == null)) {
			throw IllegalArgumentException("Exactly 1 of ownLevelRange and levelRangeName must be null")
		}
	}

	fun getLevelRange() = ownLevelRange ?: sharedLevelRange!!.range

	fun getEnemySelections() = ownEnemies ?: sharedEnemies!!.selections

	private fun monstersString() = sharedEnemies ?: ownEnemies!!.toString()

	private fun levelsString() = sharedLevelRange?: ownLevelRange!!.toString()

	override fun toString(): String {
		return "RandomBattles(monsters=${monstersString()}, levels=${levelsString()}, " +
				"minSteps=$minSteps, chance=$chance%, specialBackground=$specialBackground"
	}

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}

@BitStruct(backwardCompatible = true)
class BattleEnemySelection(

	@BitField(id = 0)
	@ReferenceField(stable = false, label = "monsters")
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(minValue = 4, maxValue = 4, expectUniform = true))
	val enemies: ArrayList<Monster?>,

	@BitField(id = 1)
	@ReferenceField(stable = false, label = "enemy party layouts")
	val enemyLayout: PartyLayout,
) {
	@Suppress("unused")
	private constructor() : this(arrayListOf(null, null, null, null), PartyLayout())

	init {
		if (enemies.size != 4) throw IllegalArgumentException("There must be exactly 4 enemy names")
	}

	override fun toString() = "EnemySelection(layout=$enemyLayout, enemies=$enemies)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}

@BitStruct(backwardCompatible = true)
class LevelRange(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val min: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val max: Int
) {
	internal constructor() : this(0, 0)

	override fun toString() = "LevelRange($min, $max)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}

@BitStruct(backwardCompatible = true)
class SharedLevelRange(

	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val range: LevelRange,
) {
	@Suppress("unused")
	private constructor() : this("", LevelRange())

	override fun toString() = "$range($name)"
}

@BitStruct(backwardCompatible = true)
class SharedEnemySelections(

	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val selections: ArrayList<BattleEnemySelection>,
) {
	constructor() : this("", ArrayList(0))

	override fun toString() = "Monsters($name)"
}
