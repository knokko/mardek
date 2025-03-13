package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import mardek.content.Content
import mardek.content.characters.PlayableCharacter
import mardek.content.combat.CombatStat
import mardek.content.combat.StatModifier
import mardek.content.combat.StatusEffect
import mardek.state.ingame.characters.CharacterState

private fun determinePlayerMaxHealth(player: PlayableCharacter, state: CharacterState) = { content: Content, bonusVitality: Int ->
	val stats = ArrayList(player.baseStats)
	if (bonusVitality != 0) stats.add(StatModifier(content.stats.stats.find { it.flashName == "VIT" }!!, bonusVitality))
	state.determineMaxHealth(stats, content.stats.stats)
}

private fun determinePlayerMaxMana(player: PlayableCharacter, state: CharacterState) = { content: Content, bonusSpirit: Int ->
	val stats = ArrayList(player.baseStats)
	if (bonusSpirit != 0) stats.add(StatModifier(content.stats.stats.find { it.flashName == "SPR" }!!, bonusSpirit))
	state.determineMaxMana(stats, content.stats.stats)
}

@BitStruct(backwardCompatible = true)
class CombatantState private constructor( // TODO Use BitField's
	var maxHealth: Int,
	var maxMana: Int,
	var currentHealth: Int,
	var currentMana: Int,

	val recomputeMaxHealth: (Content, vitModifier: Int) -> Int,
	val recomputeMaxMana: (Content, sprModifier: Int) -> Int,

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

	@Suppress("unused")
	private constructor() : this(
		0, 0, 0, 0, { _, _ -> 0 }, { _, _ -> 0 }, HashSet()
	)

	constructor(enemy: Enemy, content: Content) : this(
		maxHealth = enemy.determineMaxHealth(content.stats.stats, 0),
		recomputeMaxHealth = { _, bonusVitality -> enemy.determineMaxHealth(content.stats.stats, bonusVitality) },
		currentHealth = enemy.determineMaxHealth(content.stats.stats, 0),

		maxMana = enemy.determineMaxMana(content.stats.stats, 0),
		recomputeMaxMana = { _, bonusSpirit -> enemy.determineMaxMana(content.stats.stats, bonusSpirit) },
		currentMana = enemy.determineMaxMana(content.stats.stats, 0),

		statusEffects = HashSet(enemy.monster.initialEffects)
	)

	constructor(player: PlayableCharacter, state: CharacterState, content: Content) : this(
		maxHealth = determinePlayerMaxHealth(player, state)(content, 0),
		recomputeMaxHealth = determinePlayerMaxHealth(player, state),
		currentHealth = state.currentHealth,

		maxMana = determinePlayerMaxMana(player, state)(content, 0),
		recomputeMaxMana = determinePlayerMaxMana(player, state),
		currentMana = state.currentMana,

		statusEffects = HashSet(state.activeStatusEffects + state.determineAutoEffects())
	)
}
