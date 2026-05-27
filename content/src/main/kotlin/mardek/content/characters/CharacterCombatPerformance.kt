package mardek.content.characters

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

/**
 * This class is used by [CharacterState] to track the combat performance of its character.
 *
 * This information is shown in the party tab, but has little in-game consequences. The only noticeable exception is the
 * SinStrike skill, which deals more damage when Zach has more kills.
 */
@BitStruct(backwardCompatible = true)
class CharacterCombatPerformance {

	/**
	 * Counts how many battles this character has won
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	var numBattles = 0

	/**
	 * Counts how many enemies this character has defeated
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	var numKills = 0

	/**
	 * Counts how many times this character was defeated
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	var numFaints = 0

	/**
	 * Counts how many melee attacks this character has performed
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	var numMeleeAttacks = 0

	/**
	 * Counts how many magic/ranged attacks this character has performed *during combat*
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	var numMagicSkills = 0

	/**
	 * Counts how many consumable items this character has used *during combat*
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 0)
	var numItems = 0

	/**
	 * Counts how much damage this character has dealt (or healed) during combat. This field ignores status effects.
	 */
	@BitField(id = 6)
	@IntegerField(expectUniform = false, minValue = 0)
	var damageDealt = 0

	/**
	 * Counts how much damage this character has received during combat. This includes status effects, but excludes
	 * any healing.
	 */
	@BitField(id = 7)
	@IntegerField(expectUniform = false, minValue = 0)
	var damageReceived = 0
}
