package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CombatStat
import mardek.content.stats.StatModifier
import mardek.content.stats.StatusEffect
import mardek.state.ingame.characters.CharacterState

private fun determinePlayerMaxHealth(player: PlayableCharacter, state: CharacterState) = { bonusVitality: Int ->
	val stats = ArrayList(player.baseStats)
	if (bonusVitality != 0) stats.add(StatModifier(CombatStat.Vitality, bonusVitality))
	state.determineMaxHealth(stats)
}

private fun determinePlayerMaxMana(player: PlayableCharacter, state: CharacterState) = { bonusSpirit: Int ->
	val stats = ArrayList(player.baseStats)
	if (bonusSpirit != 0) stats.add(StatModifier(CombatStat.Spirit, bonusSpirit))
	state.determineMaxMana(stats)
}

@BitStruct(backwardCompatible = true)
class CombatantState private constructor( // TODO Use BitField's
	var maxHealth: Int,
	var maxMana: Int,
	var currentHealth: Int,
	var currentMana: Int,

	val recomputeMaxHealth: (vitModifier: Int) -> Int,
	val recomputeMaxMana: (sprModifier: Int) -> Int,

	val statusEffects: HashSet<StatusEffect>,
) {

	var lastPointedTo = 0L

	/**
	 * Incremented whenever the combatant spends a turn (e.g. by casting a spell), and reset at the end of every
	 * battle round.
	 */
	var spentTurnsThisRound = 0

	/**
	 * Temporary stat modifiers for this battle
	 */
	val statModifiers = HashMap<CombatStat, Int>()

	constructor() : this(
		0, 0, 0, 0, { _ -> 0 }, { _ -> 0 }, HashSet()
	)

	constructor(enemy: Enemy) : this(
		maxHealth = enemy.determineMaxHealth(0),
		recomputeMaxHealth = { bonusVitality -> enemy.determineMaxHealth(bonusVitality) },
		currentHealth = enemy.determineMaxHealth(0),

		maxMana = enemy.determineMaxMana(0),
		recomputeMaxMana = { bonusSpirit -> enemy.determineMaxMana(bonusSpirit) },
		currentMana = enemy.determineMaxMana(0),

		statusEffects = HashSet(enemy.monster.initialEffects)
	)

	constructor(player: PlayableCharacter, state: CharacterState) : this(
		maxHealth = determinePlayerMaxHealth(player, state)(0),
		recomputeMaxHealth = determinePlayerMaxHealth(player, state),
		currentHealth = state.currentHealth,

		maxMana = determinePlayerMaxMana(player, state)(0),
		recomputeMaxMana = determinePlayerMaxMana(player, state),
		currentMana = state.currentMana,

		statusEffects = HashSet(state.activeStatusEffects + state.determineAutoEffects())
	)
}
