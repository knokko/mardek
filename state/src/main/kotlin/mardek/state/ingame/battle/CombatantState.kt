package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import mardek.assets.Campaign
import mardek.assets.characters.PlayableCharacter
import mardek.assets.combat.CombatStat
import mardek.assets.combat.StatModifier
import mardek.assets.combat.StatusEffect
import mardek.state.ingame.characters.CharacterState

private fun determinePlayerMaxHealth(player: PlayableCharacter, state: CharacterState) = { campaign: Campaign, bonusVitality: Int ->
	val stats = ArrayList(player.baseStats)
	if (bonusVitality != 0) stats.add(StatModifier(campaign.combat.stats.find { it.flashName == "VIT" }!!, bonusVitality))
	state.determineMaxHealth(stats, campaign.combat.stats)
}

private fun determinePlayerMaxMana(player: PlayableCharacter, state: CharacterState) = { campaign: Campaign, bonusSpirit: Int ->
	val stats = ArrayList(player.baseStats)
	if (bonusSpirit != 0) stats.add(StatModifier(campaign.combat.stats.find { it.flashName == "SPR" }!!, bonusSpirit))
	state.determineMaxMana(stats, campaign.combat.stats)
}

@BitStruct(backwardCompatible = false)
class CombatantState private constructor(
	var maxHealth: Int,
	var maxMana: Int,
	var currentHealth: Int,
	var currentMana: Int,

	val recomputeMaxHealth: (Campaign, vitModifier: Int) -> Int,
	val recomputeMaxMana: (Campaign, sprModifier: Int) -> Int,

	val statusEffects: HashSet<StatusEffect>,
) {

	/**
	 * Incremented whenever the combatant spends a turn (e.g. by casting a spell), and reset at the end of every
	 * battle round.
	 */
	var spentTurnsThisRound = 0

	/**
	 * Temporary stat modifiers for this battle
	 */
	val statModifiers = HashMap<CombatStat, Int>()

//	constructor(enemy: Enemy) : this(
//		ehm, ehm, ehm, ehm, ehm, ehm, ehm
//	)

	constructor(player: PlayableCharacter, state: CharacterState, campaign: Campaign) : this(
		maxHealth = determinePlayerMaxHealth(player, state)(campaign, 0),
		recomputeMaxHealth = determinePlayerMaxHealth(player, state),
		currentHealth = state.currentHealth,

		maxMana = determinePlayerMaxMana(player, state)(campaign, 0),
		recomputeMaxMana = determinePlayerMaxMana(player, state),
		currentMana = state.currentMana,

		statusEffects = state.activeStatusEffects
	)
}
