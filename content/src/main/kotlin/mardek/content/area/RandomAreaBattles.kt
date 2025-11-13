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

/**
 * Describes the random battles that an area can have.
 */
@BitStruct(backwardCompatible = true)
class RandomAreaBattles(

	/**
	 * Exactly 1 of `ownEnemies` and `sharedEnemies` must be `null`.
	 *
	 * When `ownEnemies != null`, it describes the possible list of enemy selections that can be selected.
	 */
	@BitField(id = 0)
	@NestedFieldSetting(path = "", optional = true)
	val ownEnemies: ArrayList<BattleEnemySelection>?,

	/**
	 * Exactly 1 of `ownEnemies` and `sharedEnemies` must be `null`.
	 *
	 * When `sharedEnemies != null`, this is a reference to a list of enemy selections in `AreaContent.enemySelections`.
	 */
	@BitField(id = 1, optional = true)
	@ReferenceField(stable = false, label = "enemy selections")
	val sharedEnemies: SharedEnemySelections?,

	/**
	 * Exactly 1 of `ownLevelRange` and `sharedLevelRange` must be `null`.
	 *
	 * When `ownLevelRange != null`, it describes the possible range of levels that the monsters in random battles can
	 * have.
	 */
	@BitField(id = 2, optional = true)
	val ownLevelRange: LevelRange?,

	/**
	 * Exactly 1 of `ownLevelRange` and `sharedLevelRange` must be `null`.
	 *
	 * When `sharedLevelRange != null`, this is a reference to a list of enemy selections in `AreaContent.levelRanges`.
	 */
	@BitField(id = 3, optional = true)
	@ReferenceField(stable = false, label = "level ranges")
	val sharedLevelRange: SharedLevelRange?,

	/**
	 * The minimum number of steps between 2 random battle encounters in this area
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val minSteps: Int,

	/**
	 * This field determines how likely it is to get random battles in this area.
	 *
	 * After every step, a random battle will be encountered if and only if
	 * `random.nextInt(150 - stepsSinceLastBattle) <= chance`
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int, // Percentage after taking 1 step?

	/**
	 * The regular/default battle background for random battles in this area
	 */
	@BitField(id = 6)
	@ReferenceField(stable = true, label = "battle backgrounds")
	val defaultBackground: BattleBackground,

	/**
	 * An optional battle background that is only used in 'special' cases, for instance the dead temple backgrounds
	 * after the player has taken its elemental crystal.
	 */
	@BitField(id = 7, optional = true)
	@ReferenceField(stable = true, label = "battle backgrounds")
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

	/**
	 * Picks either `ownLevelRange` or `sharedLevelRange.range`, depending on which one is non-null
	 */
	fun getLevelRange() = ownLevelRange ?: sharedLevelRange!!.range

	/**
	 * Picks ether `ownEnemies` or `sharedEnemies.selections`, depending on which one is non-null
	 */
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

/**
 * A possible selection of enemies for a random battle
 */
@BitStruct(backwardCompatible = true)
class BattleEnemySelection(

	/**
	 * The actual enemies/monsters. This will always be an array of length 4, but the elements can be `null` to
	 * indicate that no monster is present at the enemy party position with that index.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "monsters")
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(minValue = 4, maxValue = 4, expectUniform = true))
	val enemies: ArrayList<Monster?>,

	/**
	 * The party layout/positions of the enemies/monsters
	 */
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

/**
 * Represents a possible level range for a random battle
 */
@BitStruct(backwardCompatible = true)
class LevelRange(

	/**
	 * The minimum level that each monster must have
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val min: Int,

	/**
	 * The maximum level that each monster can have
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val max: Int
) {
	internal constructor() : this(0, 0)

	override fun toString() = "LevelRange($min, $max)"

	override fun equals(other: Any?) = BITSER.deepEquals(this, other)

	override fun hashCode() = BITSER.hashCode(this)
}

/**
 * Represents a `LevelRange` that can be shared between multiple areas (typically areas in the same dungeon)
 */
@BitStruct(backwardCompatible = true)
class SharedLevelRange(

	/**
	 * The name, as imported from Flash. This is not used in-game, but is potentially useful for debugging and
	 * editing.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The actual level range
	 */
	@BitField(id = 1)
	val range: LevelRange,
) {
	@Suppress("unused")
	private constructor() : this("", LevelRange())

	override fun toString() = "$range($name)"
}

/**
 * Represents a `BattleEnemySelection` that can be shared between multiple areas (typically areas in the same dungeon)
 */
@BitStruct(backwardCompatible = true)
class SharedEnemySelections(

	/**
	 * The name, as imported from Flash. This is not used in-game, but is potentially useful for debugging and
	 * editing.
	 */
	@BitField(id = 0)
	val name: String,

	/**
	 * The actual enemy/monster selection
	 */
	@BitField(id = 1)
	val selections: ArrayList<BattleEnemySelection>,
) {
	constructor() : this("", ArrayList(0))

	override fun toString() = "Monsters($name)"
}
