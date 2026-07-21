package mardek.state.ingame

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

/**
 * This class is used to track some of the information that is displayed on the "Status" tab of the in-game menu,
 * as well as some similar information (e.g. total time spent in the save).
 *
 * Note that this class only tracks the fields that can*not* be derived from the combat performance of the
 * playable characters.
 * For instance, this class tracks [goldEarned], but *not* `damageDealt`, since the latter can be derived by summing up
 * the [mardek.content.characters.CharacterCombatPerformance.damageDealt] of all playable characters.
 */
@BitStruct(backwardCompatible = true)
class CampaignStatistics {

	/**
	 * The total number of steps/tiles that the player has walked voluntarily.
	 *
	 * This field is initially 0, and gets incremented by 1 each time the player voluntarily walks to a new tile.
	 * Forced movement (e.g. during dialogues) does *not* increment this counter.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	var totalSteps = 0L

	/**
	 * The total number of gold the player has earned.
	 *
	 * This variable is increased whenever the player loots money from a battle or chest, but not whenever the player
	 * buys or sells items.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	var goldEarned = 0

	/**
	 * The number of items that the player has consumed (e.g. potions).
	 *
	 * This includes both the items that were consumed during combat, and the items that were consumed outside combat.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	var itemsConsumed = 0

	/**
	 * The number of battles that the player has won.
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	var battlesWon = 0

	/**
	 * The number of battles from which the player ran away.
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 0)
	var battlesFled = 0

	/**
	 * The total number of monsters that were defeated.
	 *
	 * This includes monsters that were killed by poison/bleed, as well as monsters that were killed by another
	 * (confused) monster.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = 0)
	var numKills = 0
}
