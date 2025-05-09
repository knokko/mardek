package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import mardek.content.battle.StrategyPool
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item
import mardek.content.stats.CombatStat
import mardek.content.stats.Element
import mardek.content.stats.StatModifier
import mardek.content.stats.StatusEffect
import mardek.state.ingame.characters.CharacterState

private fun determinePlayerMaxHealth(
	player: PlayableCharacter, state: CharacterState
) = { bonusVitality: Int, statusEffects: Set<StatusEffect> ->
	val stats = ArrayList(player.baseStats)
	if (bonusVitality != 0) stats.add(StatModifier(CombatStat.Vitality, bonusVitality))
	state.determineMaxHealth(stats, statusEffects)
}

private fun determinePlayerMaxMana(
	player: PlayableCharacter, state: CharacterState
) = { bonusSpirit: Int, statusEffects: Set<StatusEffect> ->
	val stats = ArrayList(player.baseStats)
	if (bonusSpirit != 0) stats.add(StatModifier(CombatStat.Spirit, bonusSpirit))
	state.determineMaxMana(stats, statusEffects)
}

@BitStruct(backwardCompatible = true)
class CombatantState private constructor( // TODO Use BitField's
	var maxHealth: Int,
	var maxMana: Int,
	var currentHealth: Int,
	var currentMana: Int,

	val recomputeMaxHealth: (vitModifier: Int, statusEffects: Set<StatusEffect>) -> Int,
	val recomputeMaxMana: (sprModifier: Int, statusEffects: Set<StatusEffect>) -> Int,

	val statusEffects: HashSet<StatusEffect>,
	var element: Element,

	val equipment: Array<Item?>,
) {

	var lastPointedTo = 0L

	/**
	 * Incremented whenever the combatant spends a turn (e.g. by casting a spell), and reset at the end of every
	 * battle round.
	 */
	var spentTurnsThisRound = 0

	var totalSpentTurns = 0

	var lastMove: BattleMove? = null

	/**
	 * Temporary stat modifiers for this battle
	 */
	val statModifiers = HashMap<CombatStat, Int>()

	/**
	 * Monsters only: how often each strategy has already been used in this battle.
	 * This is needed because some strategies (e.g. Dark Gift) can only be used once per battle.
	 */
	// TODO right annotation: pool must be a stable reference
	val usedStrategies = HashMap<StrategyPool, Int>()

	@Suppress("unused")
	private constructor() : this(
		0, 0, 0, 0, { _, _ -> 0 }, { _, _ -> 0 },
		HashSet(), Element(), Array(6) { null }
	)

	constructor(enemy: Enemy) : this(
		maxHealth = enemy.determineMaxHealth(0, enemy.monster.initialEffects.toSet()),
		recomputeMaxHealth = { bonusVitality, statusEffects -> enemy.determineMaxHealth(bonusVitality, statusEffects) },
		currentHealth = enemy.determineMaxHealth(0, enemy.monster.initialEffects.toSet()),

		maxMana = enemy.determineMaxMana(0, enemy.monster.initialEffects.toSet()),
		recomputeMaxMana = { bonusSpirit, statusEffects -> enemy.determineMaxMana(bonusSpirit, statusEffects) },
		currentMana = enemy.determineMaxMana(0, enemy.monster.initialEffects.toSet()),

		statusEffects = HashSet(enemy.monster.initialEffects),
		element = enemy.monster.element,
		equipment = arrayOf(
			enemy.monster.weapon.pick(),
			enemy.monster.shield.pick(),
			enemy.monster.helmet.pick(),
			enemy.monster.armor.pick(),
			enemy.monster.accessory1.pick(),
			enemy.monster.accessory2.pick()
		),
	)

	constructor(player: PlayableCharacter, state: CharacterState) : this(
		maxHealth = determinePlayerMaxHealth(player, state)(0, state.activeStatusEffects),
		recomputeMaxHealth = determinePlayerMaxHealth(player, state),
		currentHealth = state.currentHealth,

		maxMana = determinePlayerMaxMana(player, state)(0, state.activeStatusEffects),
		recomputeMaxMana = determinePlayerMaxMana(player, state),
		currentMana = state.currentMana,

		statusEffects = HashSet(state.activeStatusEffects + state.determineAutoEffects()),
		element = player.element,
		equipment = state.equipment,
	)
}
