package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.animations.BattleModel
import mardek.content.battle.Monster
import mardek.content.battle.StrategyPool
import mardek.content.characters.PlayableCharacter
import mardek.content.inventory.Item
import mardek.content.skill.PassiveSkill
import mardek.content.skill.ReactionSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.skill.Skill
import mardek.content.sprite.KimSprite
import mardek.content.stats.*
import mardek.state.ingame.characters.CharacterState
import kotlin.math.max
import kotlin.math.min

private fun determinePlayerMaxHealth(
	player: PlayableCharacter, characterState: CharacterState,
	bonusVitality: Int, statusEffects: Set<StatusEffect>
): Int {
	val stats = ArrayList(player.baseStats)
	if (bonusVitality != 0) stats.add(StatModifier(CombatStat.Vitality, bonusVitality))
	return characterState.determineMaxHealth(stats, statusEffects)
}

private fun determinePlayerMaxMana(
	player: PlayableCharacter, state: CharacterState,
	bonusSpirit: Int, statusEffects: Set<StatusEffect>
): Int {
	val stats = ArrayList(player.baseStats)
	if (bonusSpirit != 0) stats.add(StatModifier(CombatStat.Spirit, bonusSpirit))
	return state.determineMaxMana(stats, statusEffects)
}

private fun determineEnemyMaxHealth(
	monster: Monster, level: Int, bonusVitality: Int, statusEffects: Set<StatusEffect>
): Int {
	if (monster.playerStatModifier == 0) {
		return monster.baseStats[CombatStat.MaxHealth]!! + level * monster.hpPerLevel
	} else {
		var vitality = monster.baseStats[CombatStat.Vitality] ?: 0
		var extra = monster.baseStats[CombatStat.MaxHealth] ?: 0
		vitality += bonusVitality
		for (effect in statusEffects) {
			vitality += effect.getModifier(CombatStat.Vitality)
			extra += effect.getModifier(CombatStat.MaxHealth)
		}
		return CharacterState.determineMaxHealth(level, vitality, 0f, extra)
	}
}

private fun determineEnemyMaxMana(
	monster: Monster, level: Int, bonusSpirit: Int, statusEffects: Set<StatusEffect>
): Int {
	if (monster.playerStatModifier == 0) {
		return monster.baseStats[CombatStat.MaxMana]!!
	} else {
		var spirit = monster.baseStats[CombatStat.Spirit] ?: 0
		var extra = monster.baseStats[CombatStat.MaxMana] ?: 0
		spirit += bonusSpirit
		for (effect in statusEffects) {
			spirit += effect.getModifier(CombatStat.Spirit)
			extra += effect.getModifier(CombatStat.MaxMana)
		}
		return CharacterState.determineMaxMana(level, spirit, 0f, extra)
	}
}

@BitStruct(backwardCompatible = true)
sealed class CombatantState(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	var maxHealth: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	var maxMana: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentHealth: Int,

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentMana: Int,

	@BitField(id = 4)
	@ReferenceField(stable = true, label = "status effects")
	val statusEffects: HashSet<StatusEffect>,

	@BitField(id = 5)
	@ReferenceField(stable = true, label = "elements")
	var element: Element,

	/**
	 * True if this combatant is on the side of the player, both literally (on the right of the screen) and
	 * figuratively (fights for the player in battle).
	 */
	@BitField(id = 6)
	val isOnPlayerSide: Boolean,
) {

	/**
	 * Incremented whenever the combatant spends a turn (e.g. by casting a spell), and reset at the end of every
	 * battle round. This is also incremented when the turn is skipped due to a status effect like sleep or paralysis.
	 *
	 * This is crucial for computing the turn order, and determining which combatant is on turn.
	 */
	@BitField(id = 7)
	@IntegerField(expectUniform = false, minValue = 0)
	var spentTurnsThisRound = 0

	/**
	 * Temporary stat modifiers for this battle (e.g. -5 vitality for the remainder of the battle).
	 */
	@BitField(id = 8)
	@IntegerField(expectUniform = false)
	val statModifiers = HashMap<CombatStat, Int>()

	/**
	 * The last point in time (`System.nanoTime()`) where a player pointed to this combatant as the potential target
	 * for an attack, skill, or item. This is only used to determine whether the blue 'target selection blink' should
	 * be rendered, and is not important for the course of the battle.
	 */
	var lastPointedTo = 0L

	/**
	 * This contains information that is used to render the damage indicator whenever combatants are attacked or
	 * gain/lose health or mana.
	 */
	var lastDamageIndicator: DamageIndicator? = null

	fun getPosition(battleState: BattleState) = if (isOnPlayerSide) {
		battleState.playerLayout.positions[battleState.players.indexOf(this)]
	} else battleState.battle.enemyLayout.positions[battleState.opponents.indexOf(this)]

	abstract fun computeMaxHealth(context: BattleUpdateContext): Int

	abstract fun computeMaxMana(context: BattleUpdateContext): Int

	abstract fun getEquipment(context: BattleUpdateContext): Array<Item?>

	abstract fun getStat(stat: CombatStat, context: BattleUpdateContext): Int

	fun clampHealthAndMana(context: BattleUpdateContext) {
		this.maxHealth = computeMaxHealth(context)
		this.maxMana = computeMaxMana(context)
		this.currentHealth = max(0, min(currentHealth, maxHealth))
		this.currentMana = max(0, min(currentMana, maxMana))
	}

	fun isAlive() = currentHealth > 0

	abstract fun getLevel(context: BattleUpdateContext): Int

	abstract fun getCreatureType(): CreatureType

	abstract fun getToggledSkills(context: BattleUpdateContext): Set<Skill>

	open fun getResistance(element: Element, context: BattleUpdateContext): Float {
		var resistance = 0f
		for (item in getEquipment(context)) {
			if (item?.equipment != null) resistance += item.equipment!!.resistances.get(element)
		}
		for (effect in statusEffects) resistance += effect.resistances.get(element)
		for (skill in getToggledSkills(context)) {
			if (skill is PassiveSkill) resistance += skill.resistances.get(element)
		}

		return resistance
	}

	open fun getResistance(effect: StatusEffect, context: BattleUpdateContext): Int {
		var resistance = 0
		for (item in getEquipment(context)) {
			if (item?.equipment != null) resistance += item.equipment!!.resistances.get(effect)
		}
		for (otherEffect in statusEffects) resistance += otherEffect.resistances.get(effect)
		for (skill in getToggledSkills(context)) {
			if (skill is PassiveSkill) resistance += skill.resistances.get(effect)
		}

		return resistance
	}

	fun getSosEffects(context: BattleUpdateContext): Set<StatusEffect> {
		val effects = mutableSetOf<StatusEffect>()
		for (skill in getToggledSkills(context)) {
			if (skill is PassiveSkill) effects.addAll(skill.sosEffects)
		}
		return effects
	}

	fun getAutoEffects(context: BattleUpdateContext): Set<StatusEffect> {
		val autoEffects = mutableSetOf<StatusEffect>()
		for (item in getEquipment(context)) {
			if (item?.equipment != null) autoEffects.addAll(item.equipment!!.autoEffects)
		}

		for (skill in getToggledSkills(context)) {
			if (skill is PassiveSkill) autoEffects.addAll(skill.autoEffects)
		}

		return autoEffects
	}

	abstract fun getModel(): BattleModel

	abstract fun getTurnOrderIcon(): KimSprite

	abstract fun hasReactions(context: BattleUpdateContext, type: ReactionSkillType): Boolean

	open fun transferStatusBack(context: BattleUpdateContext) {}

	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			PlayerCombatantState::class.java,
			MonsterCombatantState::class.java,
		)
	}
}

@BitStruct(backwardCompatible = true)
class PlayerCombatantState(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "playable characters")
	val player: PlayableCharacter,

	state: CharacterState,
	isOnPlayerSide: Boolean,
) : CombatantState(
	maxHealth = determinePlayerMaxHealth(player, state, 0, state.activeStatusEffects),
	currentHealth = state.currentHealth,

	maxMana = determinePlayerMaxMana(player, state, 0, state.activeStatusEffects),
	currentMana = state.currentMana,

	statusEffects = HashSet(state.activeStatusEffects + state.determineAutoEffects()),
	element = player.element,
	isOnPlayerSide = isOnPlayerSide
) {
	override fun toString() = player.name

	override fun computeMaxHealth(context: BattleUpdateContext) = determinePlayerMaxHealth(
		player, context.characterStates[player]!!,
		statModifiers.getOrDefault(CombatStat.Vitality, 0),
		statusEffects
	)

	override fun computeMaxMana(context: BattleUpdateContext) = determinePlayerMaxMana(
		player, context.characterStates[player]!!,
		statModifiers.getOrDefault(CombatStat.Spirit, 0),
		statusEffects
	)

	override fun getEquipment(context: BattleUpdateContext) = context.characterStates[player]!!.equipment

	override fun getStat(stat: CombatStat, context: BattleUpdateContext): Int {
		val extra = statModifiers.getOrDefault(stat, 0)
		val characterState = context.characterStates[player]!!
		return characterState.computeStatValue(player.baseStats, statusEffects, stat) + extra
	}

	override fun getCreatureType() = player.creatureType

	override fun getLevel(context: BattleUpdateContext) = context.characterStates[player]!!.currentLevel

	override fun getToggledSkills(context: BattleUpdateContext) = context.characterStates[player]!!.toggledSkills

	override fun getResistance(element: Element, context: BattleUpdateContext): Float {
		var resistance = super.getResistance(element, context)
		if (element === this.element) resistance += 0.2f
		if (element === this.element.weakAgainst) resistance -= 0.2f
		return resistance
	}

	override fun getModel() = player.battleModel

	override fun getTurnOrderIcon() = player.areaSprites.sprites[0]

	override fun hasReactions(
		context: BattleUpdateContext, type: ReactionSkillType
	) = context.characterStates[player]!!.toggledSkills.any { it is ReactionSkill && it.type == type }

	override fun transferStatusBack(context: BattleUpdateContext) {
		val characterState = context.characterStates[player]!!
		characterState.activeStatusEffects.clear()
		characterState.activeStatusEffects.addAll(statusEffects.filter { !it.disappearsAfterCombat })
		statusEffects.clear()
		statusEffects.addAll(characterState.activeStatusEffects)
		maxHealth = characterState.determineMaxHealth(player.baseStats, statusEffects)
		currentHealth = max(0, min(currentHealth, maxHealth))
		characterState.currentHealth = max(1, currentHealth)
		maxMana = characterState.determineMaxMana(player.baseStats, statusEffects)
		currentMana = max(0, min(currentMana, maxMana))
		characterState.currentMana = currentMana
	}
}

@BitStruct(backwardCompatible = true)
class MonsterCombatantState(

	@BitField(id = 0)
	@ReferenceField(stable = true, label = "monsters")
	val monster: Monster,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	private val level: Int,

	isOnPlayerSide: Boolean,
) : CombatantState(
	maxHealth = determineEnemyMaxHealth(monster, level, 0, monster.initialEffects.toSet()),
	currentHealth = determineEnemyMaxHealth(monster, level, 0, monster.initialEffects.toSet()),

	maxMana = determineEnemyMaxMana(monster, level, 0, monster.initialEffects.toSet()),
	currentMana = determineEnemyMaxMana(monster, level, 0, monster.initialEffects.toSet()),

	statusEffects = HashSet(monster.initialEffects),
	element = monster.element,
	isOnPlayerSide = isOnPlayerSide,
) {

	@BitField(id = 2)
	@ReferenceField(stable = true, label = "items")
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 6, maxValue = 6))
	private val equipment = arrayOf(
		monster.weapon.pick(),
		monster.shield.pick(),
		monster.helmet.pick(),
		monster.armor.pick(),
		monster.accessory1.pick(),
		monster.accessory2.pick()
	)

	/**
	 * How often each strategy has already been used in this battle.
	 * This is needed because some strategies (e.g. Dark Gift) can only be used once per battle.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 1)
	@NestedFieldSetting(path = "c", fieldName = "USED_STRATEGIES_KEY_PROPERTIES")
	val usedStrategies = HashMap<StrategyPool, Int>()

	/**
	 * Just like `spentTurnsThisRound`, this counter is incremented whenever the combatant spends a turn. But, unlike
	 * `spentTurnsThisRound`, this counter is never reset.
	 *
	 * This is needed for move selection criteria that require a move to be used only on e.g. odd turns.
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	var totalSpentTurns = 0

	/**
	 * Since monsters are not allowed to cast some moves twice in a row, we need to remember the last move that was
	 * used by the monster.
	 */
	@BitField(id = 5, optional = true)
	@ClassField(root = BattleStateMachine::class)
	var lastMove: BattleStateMachine.Move? = null

	constructor() : this(Monster(), 0, false)

	override fun toString() = "${monster.name} level $level"

	override fun computeMaxHealth(context: BattleUpdateContext) = determineEnemyMaxHealth(
		monster, level, statModifiers.getOrDefault(CombatStat.Vitality, 0), statusEffects
	)

	override fun computeMaxMana(context: BattleUpdateContext) = determineEnemyMaxMana(
		monster, level, statModifiers.getOrDefault(CombatStat.Spirit, 0), statusEffects
	)

	override fun getEquipment(context: BattleUpdateContext) = equipment

	override fun getStat(stat: CombatStat, context: BattleUpdateContext): Int {
		return monster.baseStats.getOrDefault(stat, 0) + statModifiers.getOrDefault(stat, 0)
	}

	override fun getCreatureType() = monster.type

	override fun getLevel(context: BattleUpdateContext) = level

	override fun getToggledSkills(context: BattleUpdateContext) = emptySet<Skill>()

	override fun getResistance(element: Element, context: BattleUpdateContext): Float {
		var resistance = super.getResistance(element, context)
		val shiftResistances = monster.elementalShiftResistances[this.element]
		resistance += shiftResistances?.get(element) ?: monster.resistances.get(element)
		return resistance
	}

	override fun getResistance(effect: StatusEffect, context: BattleUpdateContext): Int {
		var resistance = super.getResistance(effect, context)
		val shiftResistances = monster.elementalShiftResistances[element]
		resistance += shiftResistances?.get(effect) ?: monster.resistances.get(effect)
		return resistance
	}

	override fun getModel() = monster.model

	override fun getTurnOrderIcon() = monster.type.icon

	override fun hasReactions(context: BattleUpdateContext, type: ReactionSkillType) = false

	companion object {

		@JvmStatic
		@Suppress("unused")
		@ReferenceField(stable = true, label = "strategy pools")
		val USED_STRATEGIES_KEY_PROPERTIES = false
	}
}
