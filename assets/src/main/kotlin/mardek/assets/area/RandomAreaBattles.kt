package mardek.assets.area

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class RandomAreaBattles(

	@CollectionField
	@BitField(ordering = 0, optional = true)
	val ownEnemies: ArrayList<BattleEnemySelection>?,

	@BitField(ordering = 1, optional = true)
	val monstersTableName: String?,

	@BitField(ordering = 2, optional = true)
	val ownLevelRange: LevelRange?,

	@BitField(ordering = 3, optional = true)
	val levelRangeName: String?,

	@BitField(ordering = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val minSteps: Int,

	@BitField(ordering = 5)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int, // Percentage after taking 1 step?

	@BitField(ordering = 6, optional = true)
	val specialBackground: String?
) {

	@Suppress("unused")
	private constructor() : this(
		null, "", null, "",
		0, 0, null
	)

	init {
		if ((ownEnemies == null) == (monstersTableName == null)) {
			throw IllegalArgumentException("Exactly 1 of ownEnemies and monstersTableName must be null")
		}
		if ((ownLevelRange == null) == (levelRangeName == null)) {
			throw IllegalArgumentException("Exactly 1 of ownLevelRange and levelRangeName must be null")
		}
	}

	private fun monstersString() = monstersTableName ?: ownEnemies!!.toString()

	private fun levelsString() = levelRangeName ?: ownLevelRange!!.toString()

	override fun toString(): String {
		return "RandomBattles(monsters=${monstersString()}, levels=${levelsString()}, " +
				"minSteps=$minSteps, chance=$chance%, specialBackground=$specialBackground"
	}

	override fun equals(other: Any?) = other is RandomAreaBattles && monstersTableName == other.monstersTableName &&
			ownEnemies == other.ownEnemies && levelRangeName == other.levelRangeName &&
			ownLevelRange == other.ownLevelRange && minSteps == other.minSteps && chance == other.chance &&
			specialBackground == other.specialBackground

	override fun hashCode(): Int {
		var result = ownEnemies?.hashCode() ?: 0
		result = 31 * result + (monstersTableName?.hashCode() ?: 0)
		result = 31 * result + (ownLevelRange?.hashCode() ?: 0)
		result = 31 * result + (levelRangeName?.hashCode() ?: 0)
		result = 31 * result + minSteps
		result = 31 * result + chance
		result = 31 * result + (specialBackground?.hashCode() ?: 0)
		return result
	}
}

@BitStruct(backwardCompatible = false)
class BattleEnemySelection(

	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	@CollectionField(size = IntegerField(minValue = 4, maxValue = 4, expectUniform = true), optionalValues = true)
	val enemyNames: ArrayList<String?>
) {

	@Suppress("unused")
	private constructor() : this("", arrayListOf(null, null, null, null))

	init {
		if (enemyNames.size != 4) throw IllegalArgumentException("There must be exactly 4 enemy names")
	}

	override fun toString() = "EnemySelection(name=$name, enemies=$enemyNames)"

	override fun equals(other: Any?) = other is BattleEnemySelection && name == other.name &&
			enemyNames == other.enemyNames

	override fun hashCode() = name.hashCode() + 31 * enemyNames.hashCode()
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
	@Suppress("unused")
	private constructor() : this(0, 0)
}
