package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.animation.CombatantAnimations
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
import mardek.content.characters.CharacterState
import mardek.content.skill.ActiveSkill
import java.util.EnumMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

/**
 * Represents the (in-battle) state of a combatant.
 *
 * This is a sealed class with two subclasses:
 * - [PlayerCombatantState] for player-controlled combatants, and
 * - [MonsterCombatantState] for computer-controlled combatants (normally enemies)
 */
@BitStruct(backwardCompatible = true)
sealed class CombatantState(

	/**
	 * The maximum health of the combatant.
	 *
	 * This will be recomputed when e.g. the Vitality of the combatant is changed.
	 *
	 * For [PlayerCombatantState]s, this is usually equal to the maximum health of the player, but not always
	 * (e.g. due to vitality changes). When the battle is finished, the maximum health of the player will be reverted.
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	var maxHealth: Int,

	/**
	 * The maximum mana of the combatant.
	 *
	 * This will be recomputed when e.g. the Spirit of the combatant is changed.
	 *
	 * For [PlayerCombatantState]s, this is usually equal to the maximum health of the player, but not always
	 * (e.g. when the spirit is increased or decreased). When the battle is finished, the maximum mana of the player
	 * will be reverted.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	var maxMana: Int,

	/**
	 * The current HP of the combatant, which must always be in the range [0, [maxHealth]]. The combatant faints when
	 * its HP reaches 0.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentHealth: Int,

	/**
	 * The current MP of the combatant, which must always be in the range [0, [maxMana]]. Mana is needed to use some
	 * skills.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	var currentMana: Int,

	/**
	 * The current status effects of the combatant.
	 *
	 * For [PlayerCombatantState]s, this replaces the [CharacterState.activeStatusEffects] at the end of the battle
	 * (except for the effects with [StatusEffect.disappearsAfterCombat] = true).
	 */
	@BitField(id = 4)
	@ReferenceField(stable = true, label = "status effects")
	val statusEffects: HashSet<StatusEffect>,

	/**
	 * The current element of the combatant. This will almost always be the original element of the combatant, but some
	 * monsters (Master Stone and Karnos) can change their own element during combat.
	 */
	@BitField(id = 5)
	@ReferenceField(stable = true, label = "elements")
	var element: Element,

	/**
	 * True if this combatant is on the side of the player, both literally (on the right of the screen) and
	 * figuratively (fights for the player in battle).
	 * - This must be `true` for any combatant in [BattleState.players].
	 * - This must be `false` for any combatant in [BattleState.opponents].
	 *
	 * Normally, all combatants in [BattleState.players] will be [PlayerCombatantState]s, and all
	 * combatants in [BattleState.opponents] will be [MonsterCombatantState]s, but this is **not** required by the
	 * engine: the engine allows [BattleState.players] to contain [MonsterCombatantState]s, but this currently
	 * never happens.
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
	val statModifiers = EnumMap<CombatStat, Int>(CombatStat::class.java)

	/**
	 * This information is used to coordinate information to & from the renderer. Some coordination is needed for e.g.
	 * rendering animations.
	 */
	val renderInfo = CombatantRenderInfo()

	/**
	 * Gets the [mardek.content.battle.PartyLayoutPosition] of this combatant. This is the 'base' position where this
	 * combatant should be rendered (except when it is in the middle of e.g. a melee attack).
	 */
	fun getPosition(battleState: BattleState) = if (isOnPlayerSide) {
		battleState.playerLayout.positions[battleState.players.indexOf(this)]
	} else battleState.battle.enemyLayout.positions[battleState.opponents.indexOf(this)]

	/**
	 * Computes the maximum health that this combatant should have. This can depend on e.g. the current equipment and
	 * vitality. This should always be equal to [maxHealth].
	 */
	abstract fun computeMaxHealth(context: BattleUpdateContext): Int

	/**
	 * Computes the maximum mana that this combatant should have. This can depend on e.g. the current equipment and
	 * spirit. This should always be equal to [maxMana].
	 */
	abstract fun computeMaxMana(context: BattleUpdateContext): Int

	/**
	 * Gets the equipment of this combatant.
	 *
	 * Currently, this array should always have a length of 6, but may contain `null` elements. However, this is
	 * subject to change in the future.
	 */
	abstract fun getEquipment(context: BattleUpdateContext): Array<Item?>

	/**
	 * Gets the weapon of the combatant, or `null` if it is unarmed.
	 */
	abstract fun getWeapon(context: BattleUpdateContext): Item?

	/**
	 * Gets the base/natural [stat] value. The difference between this and [getStat] is shown with a "+" or "-" in the
	 * combatant info pop-up.
	 *
	 * For instance, if `getNatural(CombatStat.Strength) == 10` and `getStat(CombatStat.Strength, context) == 15`,
	 * the popup will show a green "15" and a "+5".
	 */
	abstract fun getNatural(stat: CombatStat): Int

	/**
	 * Gets the current [stat] value. This is the value used for e.g. damage calculations.
	 */
	abstract fun getStat(stat: CombatStat, context: BattleUpdateContext): Int

	/**
	 * Updates [maxHealth] and [maxMana], and clamps [currentHealth] and [currentMana] to them. Furthermore, this
	 * method may activate SOS effects, and it will remove non-auto status effect when [currentHealth] is 0.
	 */
	fun clampHealthAndMana(context: BattleUpdateContext) {
		this.maxHealth = computeMaxHealth(context)
		this.maxMana = computeMaxMana(context)
		this.currentHealth = max(0, min(currentHealth, maxHealth))
		this.currentMana = max(0, min(currentMana, maxMana))

		val currentTime = System.nanoTime()
		if (isAlive()) {
			if (currentHealth <= maxHealth / 5) {
				val sosEffects = getSosEffects(context) - statusEffects
				statusEffects.addAll(sosEffects)
				for (effect in sosEffects) renderInfo.effectHistory.add(effect, currentTime)
			}
		} else {
			for (effect in statusEffects - getAutoEffects(context)) {
				statusEffects.remove(effect)
				renderInfo.effectHistory.remove(effect, currentTime)
			}
		}
	}

	/**
	 * Checks whether this combatant is 'alive' or 'not fainted': it just checks whether `currentHealth > 0`.
	 */
	fun isAlive() = currentHealth > 0

	/**
	 * Checks whether healing against this combatant should be reverted. This is `true` for undead combatants and
	 * zombified combatants.
	 */
	fun revertsHealing() = getCreatureType().revertsHealing || statusEffects.any { it.isZombie }

	/**
	 * Gets the current level of this combatant
	 */
	abstract fun getLevel(context: BattleUpdateContext): Int

	/**
	 * Gets the display name of this combatant
	 */
	abstract fun getName(): String

	/**
	 * Gets the (RPG) class name of this combatant (e.g. `Pyromancer`, *not* `PlayerCombatantState`).
	 *
	 * This is displayed in the combatant info pop-up, but doesn't serve any other purpose.
	 */
	abstract fun getClassName(): String

	/**
	 * Gets the creature type (race) of this combatant. This is used to determine whether `QUARRY: XXX` reaction
	 * skills apply.
	 */
	abstract fun getCreatureType(): CreatureType

	/**
	 * Gets the [PassiveSkill]s and [ReactionSkill]s that this combatant has toggled. Currently, this can only be
	 * non-empty for [PlayerCombatantState]s.
	 */
	abstract fun getToggledSkills(context: BattleUpdateContext): Set<Skill>

	/**
	 * Gets the current resistance against [element], as a fraction (e.g. 1f means 100% resistance/complete immunity).
	 */
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

	/**
	 * Gets the current resistance against [effect], as a fraction (e.g. 1f means 100% resistance/complete immunity).
	 */
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

	/**
	 * Gets all the SOS status effects of this combatant: these status effects will be given to the combatant when its
	 * HP drops below 20% for the first time.
	 */
	fun getSosEffects(context: BattleUpdateContext): Set<StatusEffect> {
		val effects = mutableSetOf<StatusEffect>()
		for (skill in getToggledSkills(context)) {
			if (skill is PassiveSkill) effects.addAll(skill.sosEffects)
		}
		return effects
	}

	/**
	 * Gets all the auto effects of this combatant: these status effects will be given to the combatant at the start
	 * of the battle, and can **not** be removed.
	 */
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

	/**
	 * Gets all the animations of this combatant, which is primarily interesting for the renderer.
	 */
	abstract fun getAnimations(): CombatantAnimations

	/**
	 * Gets the icon that should be rendered in the turn order bar (top of the screen) to represent a turn of this
	 * combatant.
	 */
	abstract fun getTurnOrderIcon(): KimSprite

	/**
	 * Checks whether this combatant has toggled any reactions skills of type [type]
	 */
	abstract fun hasReactions(context: BattleUpdateContext, type: ReactionSkillType): Boolean

	/**
	 * This should be called when the battle is finished. [PlayerCombatantState]s should transfer all the status
	 * effects to the corresponding [CharacterState]s, except those with `disappearsAfterCombat = true`.
	 */
	open fun transferStatusBack(context: BattleUpdateContext) {}

	/**
	 * Gives experience to this combatant.
	 *
	 * When this combatant is a player, their [CharacterState.gainExperience] method will be invoked, and their
	 * current HP/MP may be increased (in case of a level-up). Furthermore, this will cause a 'gained XP' indicator to
	 * be rendered.
	 *
	 * If this player has skills that increase their EXP gain, this method will increase [rawAmount] accordingly.
	 *
	 * When this combatant is a monster, this method does nothing.
	 */
	open fun gainExperience(context: BattleUpdateContext, rawAmount: Int) {}

	/**
	 * - For players, this increments the mastery points of the toggled unmastered reaction skills of the given type.
	 * - For monsters, this does nothing.
	 */
	open fun incrementReactionSkillsMastery(context: BattleUpdateContext, type: ReactionSkillType) {}

	/**
	 * - For players, this increments the mastery points of all the toggled unmastered passive skills.
	 * - For monsters, this does nothing.
	 */
	open fun incrementPassiveSkillsMastery(context: BattleUpdateContext) {}

	/**
	 * - For players, this increments the mastery points of `skill`.
	 * - For monsters, this does nothing.
	 */
	open fun incrementActiveSkillMastery(context: BattleUpdateContext, skill: ActiveSkill) {}

	companion object {

		@JvmStatic
		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			PlayerCombatantState::class.java,
			MonsterCombatantState::class.java,
		)
	}
}

/**
 * The subclass of [CombatantState] for combatants that represent playable characters, and can be controlled by the
 * player.
 *
 * These should always fight on the side of the player, although the engine doesn't require this. (It just doesn't
 * make sense to have a player-controlled combatant on the enemy side...)
 */
@BitStruct(backwardCompatible = true)
class PlayerCombatantState(

	/**
	 * The playable character that is represented
	 */
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

	/**
	 * The skills that [player] has mastered during *this* battle. When this is non-empty for at least one player, all
	 * the skills mastered during the battle will be shown after the loot screen.
	 */
	@BitField(id = 1)
	@ReferenceField(stable = true, label = "skills")
	val masteredSkillsThisBattle = HashSet<Skill>()

	/**
	 * This field tracks the recently-gained experience of this player, and helps the renderer with deciding how to
	 * display it.
	 */
	val experienceIndicators = ExperienceIndicators()

	/**
	 * The most recent level-up that happened during this battle, or `null` if no level-up has happened yet.
	 * When this is non-null and not too long ago, the renderer should display a "Level up" indicator at the character
	 */
	var lastLevelUp: LevelUpIndicator? = null
		private set

	constructor() : this(PlayableCharacter(), CharacterState(), true)

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

	override fun getEquipment(context: BattleUpdateContext): Array<Item?> {
		val equipment = context.characterStates[player]!!.equipment
		return player.characterClass.equipmentSlots.map { equipment[it] }.toTypedArray()
	}

	override fun getWeapon(context: BattleUpdateContext) = getEquipment(context).find {
		it != null && it.type.displayName.contains("WEAPON")
	}

	override fun getNatural(stat: CombatStat): Int {
		var result = 0
		for (modifier in player.baseStats) {
			if (modifier.stat == stat) result += modifier.adder
		}
		return result
	}

	override fun getStat(stat: CombatStat, context: BattleUpdateContext): Int {
		val extra = statModifiers.getOrDefault(stat, 0)
		val characterState = context.characterStates[player]!!
		return characterState.computeStatValue(player.baseStats, statusEffects, stat) + extra
	}

	override fun getName() = player.name

	override fun getClassName() = player.characterClass.displayName

	override fun getCreatureType() = player.creatureType

	override fun getLevel(context: BattleUpdateContext) = context.characterStates[player]!!.currentLevel

	override fun getToggledSkills(context: BattleUpdateContext) = context.characterStates[player]!!.toggledSkills

	override fun getResistance(element: Element, context: BattleUpdateContext): Float {
		var resistance = super.getResistance(element, context)
		if (element === this.element) resistance += 0.2f
		if (element === this.element.weakAgainst) resistance -= 0.2f
		return resistance
	}

	override fun getAnimations() = player.animations

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

	override fun gainExperience(context: BattleUpdateContext, rawAmount: Int) {
		val state = context.characterStates[player]!!
		var expFactor = 1f
		for (skill in state.toggledSkills) {
			if (skill is PassiveSkill) expFactor += skill.experienceModifier
		}
		val amount = (expFactor * rawAmount / getLevel(context)).roundToInt()

		val oldLevel = state.currentLevel
		val oldMaxHealth = maxHealth
		val oldMaxMana = maxMana

		state.gainExperience(amount)
		experienceIndicators.queuedAmount += amount

		if (state.currentLevel > oldLevel) {
			clampHealthAndMana(context) // Recompute maxHealth and maxMana
			currentHealth += maxHealth - oldMaxHealth
			currentMana += maxMana - oldMaxMana
			clampHealthAndMana(context)

			context.soundQueue.insert(context.sounds.battle.levelUp)
			lastLevelUp = LevelUpIndicator(System.nanoTime(), state.currentLevel)
		}
	}

	private fun incrementSkillMastery(context: BattleUpdateContext, playerState: CharacterState, skill: Skill) {
		val oldMastery = playerState.skillMastery[skill] ?: 0
		if (oldMastery < skill.masteryPoints) {
			val newMastery = oldMastery + 1
			playerState.skillMastery[skill] = newMastery
			if (newMastery == skill.masteryPoints) {
				masteredSkillsThisBattle.add(skill)
				context.soundQueue.insert(context.sounds.battle.masteredSkill)
			}
		}
	}

	override fun incrementReactionSkillsMastery(context: BattleUpdateContext, type: ReactionSkillType) {
		val playerState = context.characterStates[player]!!
		for (skill in playerState.toggledSkills) {
			if (skill is ReactionSkill && skill.type == type) {
				incrementSkillMastery(context, playerState, skill)
			}
		}
	}

	override fun incrementPassiveSkillsMastery(context: BattleUpdateContext) {
		val playerState = context.characterStates[player]!!
		for (skill in playerState.toggledSkills) {
			if (skill is PassiveSkill) incrementSkillMastery(context, playerState, skill)
		}
	}

	override fun incrementActiveSkillMastery(context: BattleUpdateContext, skill: ActiveSkill) {
		val playerState = context.characterStates[player]!!
		incrementSkillMastery(context, playerState, skill)
	}
}

/**
 * The subclass of [CombatantState] that is used for combatants that are *not* controlled by the player. This is
 * usually for monsters, but it can also be used for human opponents like bandits.
 *
 * These combatants are normally the opponents of the player, but this engine also allows them to fight on the same
 * side as the player (but this 'feature' is currently unused).
 */
@BitStruct(backwardCompatible = true)
class MonsterCombatantState(

	/**
	 * The content [Monster]. Note that multiple [MonsterCombatantState]s can have the same monster.
	 */
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "monsters")
	val monster: Monster,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	private val level: Int,

	isOnPlayerSide: Boolean,

	/**
	 * When this is non-null, it will be used instead of `monster.displayName`.
	 */
	@BitField(id = 2, optional = true)
	val overrideDisplayName: String?,
) : CombatantState(
	maxHealth = determineEnemyMaxHealth(monster, level, 0, monster.initialEffects.toSet()),
	currentHealth = determineEnemyMaxHealth(monster, level, 0, monster.initialEffects.toSet()),

	maxMana = determineEnemyMaxMana(monster, level, 0, monster.initialEffects.toSet()),
	currentMana = determineEnemyMaxMana(monster, level, 0, monster.initialEffects.toSet()),

	statusEffects = HashSet(monster.initialEffects),
	element = monster.element,
	isOnPlayerSide = isOnPlayerSide,
) {

	@BitField(id = 3)
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
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 1)
	@NestedFieldSetting(path = "c", fieldName = "USED_STRATEGIES_KEY_PROPERTIES")
	val usedStrategies = HashMap<StrategyPool, Int>()

	/**
	 * Just like `spentTurnsThisRound`, this counter is incremented whenever the combatant spends a turn. But, unlike
	 * `spentTurnsThisRound`, this counter is never reset.
	 *
	 * This is needed for move selection criteria that require a move to be used only on e.g. odd turns.
	 */
	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 0)
	var totalSpentTurns = 0

	/**
	 * Since monsters are not allowed to cast some moves twice in a row, we need to remember the last move that was
	 * used by the monster.
	 */
	@BitField(id = 6, optional = true)
	@ClassField(root = BattleStateMachine::class)
	var lastMove: BattleStateMachine.Move? = null

	constructor() : this(Monster(), 0, false, null)

	override fun toString() = "${monster.name} level $level"

	override fun computeMaxHealth(context: BattleUpdateContext) = determineEnemyMaxHealth(
		monster, level, statModifiers.getOrDefault(CombatStat.Vitality, 0), statusEffects
	)

	override fun computeMaxMana(context: BattleUpdateContext) = determineEnemyMaxMana(
		monster, level, statModifiers.getOrDefault(CombatStat.Spirit, 0), statusEffects
	)

	override fun getEquipment(context: BattleUpdateContext) = equipment

	override fun getWeapon(context: BattleUpdateContext) = equipment[0]

	override fun getNatural(stat: CombatStat) = monster.baseStats.getOrDefault(stat, 0)!!

	override fun getStat(stat: CombatStat, context: BattleUpdateContext): Int {
		var result = monster.baseStats.getOrDefault(stat, 0)
		result += statModifiers.getOrDefault(stat, 0)
		if (stat == CombatStat.Attack && monster.attackPerLevelDenominator != 0) {
			result += monster.attackPerLevelNumerator * (level / monster.attackPerLevelDenominator)
		}
		return result
	}

	override fun getName() = monster.name

	override fun getClassName() = monster.className

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

	override fun getAnimations() = monster.animations

	override fun getTurnOrderIcon() = monster.type.icon

	override fun hasReactions(context: BattleUpdateContext, type: ReactionSkillType) = false

	companion object {

		@Suppress("unused")
		@ReferenceField(stable = true, label = "strategy pools")
		private const val USED_STRATEGIES_KEY_PROPERTIES = false
	}
}

/**
 * When the turn of a combatant is forcibly skipped (e.g. due to paralysis or numbness + berserk),
 * this class remembers the time at which the turn was skipped, as well as the right blink color. This is the type of
 * [CombatantRenderInfo.lastForcedTurn].
 *
 * This information is used by the renderer to show the yellow/red paralysis/numbness blink/flash.
 */
class ForcedTurnBlink(

	/**
	 * The blink color (use `ColorPacker` to extract the RGB)
	 */
	val color: Int
) {

	/**
	 * The result of `System.nanoTime()` when the turn was skipped (and the blink started)
	 */
	val time = System.nanoTime()
}
