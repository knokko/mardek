package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.battle.BattleBackground
import mardek.assets.battle.Monster
import mardek.assets.battle.PartyLayout

@BitStruct(backwardCompatible = false)
class RandomAreaBattles(

	@BitField(ordering = 0)
	@NestedFieldSetting(path = "", optional = true)
	val ownEnemies: ArrayList<BattleEnemySelection>?,

	@BitField(ordering = 1, optional = true)
	@ReferenceField(stable = false, label = "enemy selections")
	val sharedEnemies: SharedEnemySelections?,

	@BitField(ordering = 2, optional = true)
	val ownLevelRange: LevelRange?,

	@BitField(ordering = 3, optional = true)
	@ReferenceField(stable = false, label = "level ranges")
	val sharedLevelRange: SharedLevelRange?,

	@BitField(ordering = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val minSteps: Int,

	@BitField(ordering = 5)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int, // Percentage after taking 1 step?

	@BitField(ordering = 6)
	val defaultBackground: BattleBackground,

	@BitField(ordering = 7, optional = true)
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

	override fun equals(other: Any?) = other is RandomAreaBattles && sharedEnemies == other.sharedEnemies &&
			ownEnemies == other.ownEnemies && sharedLevelRange == other.sharedLevelRange &&
			ownLevelRange == other.ownLevelRange && minSteps == other.minSteps && chance == other.chance &&
			specialBackground == other.specialBackground

	override fun hashCode(): Int {
		var result = ownEnemies?.hashCode() ?: 0
		result = 31 * result + (sharedEnemies?.hashCode() ?: 0)
		result = 31 * result + (ownLevelRange?.hashCode() ?: 0)
		result = 31 * result + (sharedLevelRange?.hashCode() ?: 0)
		result = 31 * result + minSteps
		result = 31 * result + chance
		result = 31 * result + (specialBackground?.hashCode() ?: 0)
		return result
	}
}

@BitStruct(backwardCompatible = false)
class BattleEnemySelection(

	@BitField(ordering = 0)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(minValue = 4, maxValue = 4, expectUniform = true))
	val enemies: ArrayList<Monster?>,

	@BitField(ordering = 1)
	@ReferenceField(stable = false, label = "enemy party layouts")
	val enemyLayout: PartyLayout,
) {
	@Suppress("unused")
	private constructor() : this(arrayListOf(null, null, null, null), PartyLayout())

	init {
		if (enemies.size != 4) throw IllegalArgumentException("There must be exactly 4 enemy names")
	}

	override fun toString() = "EnemySelection(layout=$enemyLayout, enemies=$enemies)"

	override fun equals(other: Any?) = other is BattleEnemySelection && enemyLayout == other.enemyLayout &&
			enemies == other.enemies

	override fun hashCode() = enemyLayout.hashCode() + 31 * enemies.hashCode()
}

@BitStruct(backwardCompatible = false)
class LevelRange(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val min: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val max: Int
) {
	internal constructor() : this(0, 0)

	override fun toString() = "LevelRange($min, $max)"

	override fun equals(other: Any?) = other is LevelRange && min == other.min && max == other.max

	override fun hashCode() = min - 127 * max
}

@BitStruct(backwardCompatible = false)
class SharedLevelRange(

	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val range: LevelRange,
) {
	@Suppress("unused")
	private constructor() : this("", LevelRange())

	override fun toString() = "$range($name)"
}

@BitStruct(backwardCompatible = false)
class SharedEnemySelections(

	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val selections: ArrayList<BattleEnemySelection>,
) {
	constructor() : this("", ArrayList(0))

	override fun toString() = "Monsters($name)"
}
