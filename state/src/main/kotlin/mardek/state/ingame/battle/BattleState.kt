package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.battle.PartyLayout
import mardek.content.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set

@BitStruct(backwardCompatible = true)
class BattleState(
	@BitField(id = 0)
	val battle: Battle,

	players: Array<PlayableCharacter?>,

	@BitField(id = 1)
	val playerLayout: PartyLayout,

	context: BattleUpdateContext,
) {

	@BitField(id = 2)
	@ClassField(root = CombatantState::class)
	@ReferenceFieldTarget(label = "combatants")
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val players: Array<CombatantState?> = players.map { player ->
		if (player != null) PlayerCombatantState(player, context.characterStates[player]!!, true) else null
	}.toTypedArray()

	@BitField(id = 3)
	@ClassField(root = CombatantState::class)
	@ReferenceFieldTarget(label = "combatants")
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val opponents: Array<CombatantState?> = battle.startingEnemies.map { enemy ->
		if (enemy != null) MonsterCombatantState(enemy.monster, enemy.level, false) else null
	}.toTypedArray()

	@BitField(id = 4)
	@ClassField(root = BattleStateMachine::class)
	var state: BattleStateMachine = BattleStateMachine.NextTurn(System.nanoTime() + 750_000_000L)

	val startTime = System.nanoTime()

	val particles = mutableListOf<ParticleEffectState>()

	/**
	 * The last mouse position that was detected (after receiving a `MouseMoveEvent`), in pixels
	 */
	var lastMousePosition: Pair<Int, Int>? = null

	/**
	 * When clicking at or around the health bar of a combatant, a modal/pop-up will open, which will display all
	 * kinds of information about the combatant (like strength, elemental resistances, and status effect resistances).
	 *
	 * When this is non-null, the information about this combatant will be displayed.
	 */
	var openCombatantInfo: CombatantState? = null

	@Suppress("unused")
	internal constructor() : this(Battle(), arrayOf(null, null, null, null), PartyLayout(), BattleUpdateContext())

	fun allPlayers() = players.filterNotNull()

	fun livingPlayers() = allPlayers().filter { it.isAlive() }

	fun allOpponents() = opponents.filterNotNull()

	fun livingOpponents() = allOpponents().filter { it.isAlive() }

	internal fun confirmMove(context: BattleUpdateContext, newState: BattleStateMachine) {
		this.state = newState
		if (newState is BattleStateMachine.Wait) context.soundQueue.insert(context.sounds.ui.clickCancel)
		else context.soundQueue.insert(context.sounds.ui.clickConfirm)
	}

	fun getReactionChallenge(): ReactionChallenge? {
		val state = this.state
		return when (state) {
			is BattleStateMachine.MeleeAttack -> state.reactionChallenge
			is BattleStateMachine.CastSkill -> state.reactionChallenge
			else -> null
		}
	}

	fun processKeyPress(key: InputKey, context: BattleUpdateContext) {
		val state = this.state
		val openCombatantInfo = this.openCombatantInfo
		val reactionChallenge = this.getReactionChallenge()
		if (state is BattleStateMachine.SelectMove && openCombatantInfo == null) {
			if (key == InputKey.Cancel) battleCancel(this, context)
			if (key == InputKey.Interact) battleClick(this, context)
			if (key == InputKey.MoveLeft || key == InputKey.MoveRight) battleScrollHorizontally(this, key, context)
			if (key == InputKey.MoveUp || key == InputKey.MoveDown) battleScrollVertically(this, key, context)
		}
		if (key == InputKey.Interact && reactionChallenge != null && reactionChallenge.clickedAfter == -1L) {
			reactionChallenge.clickedAfter = System.nanoTime() - reactionChallenge.startTime
		}

		if (key == InputKey.Click) {
			val mouse = this.lastMousePosition
			for (combatant in allPlayers() + allOpponents()) {
				val renderRegion = combatant.renderedInfoBlock
				if (mouse != null && renderRegion != null && renderRegion.contains(mouse.first, mouse.second)) {
					this.openCombatantInfo = combatant
					context.soundQueue.insert(context.sounds.ui.clickConfirm)
					break
				}
			}
			if (this.openCombatantInfo == openCombatantInfo) {
				this.openCombatantInfo = null
				context.soundQueue.insert(context.sounds.ui.clickCancel)
			}
		}

		if (openCombatantInfo != null && (key == InputKey.Interact || key == InputKey.Cancel || key == InputKey.Escape)) {
			this.openCombatantInfo = null
			context.soundQueue.insert(context.sounds.ui.clickCancel)
		}
	}

	fun processMouseMove(event: MouseMoveEvent) {
		this.lastMousePosition = Pair(event.newX, event.newY)
	}

	private fun nextCombatantOnTurn(context: BattleUpdateContext): CombatantState? {
		val combatants = livingPlayers() + livingOpponents()
		if (combatants.none { it.isOnPlayerSide }) state = BattleStateMachine.GameOver()
		if (combatants.none { !it.isOnPlayerSide }) state = BattleStateMachine.Victory()
		if (state is BattleStateMachine.GameOver || state is BattleStateMachine.Victory) return null

		val simulator = TurnOrderSimulator(this, context)
		if (simulator.checkReset()) {
			for (combatant in combatants) combatant.spentTurnsThisRound = 0
		}
		return simulator.next()
	}

	private fun beginTurn(context: BattleUpdateContext, combatant: CombatantState) {
		combatant.spentTurnsThisRound += 1
		if (combatant is MonsterCombatantState) combatant.totalSpentTurns += 1
		// TODO Allow status effects to skip the turn, or to deal damage

		state = if (combatant is PlayerCombatantState) {
			context.soundQueue.insert(context.sounds.ui.partyScroll)
			BattleStateMachine.SelectMove(combatant)
		} else {
			MonsterStrategyCalculator(
				this, combatant as MonsterCombatantState, context
			).determineNextMove() as BattleStateMachine
		}
	}

	fun update(context: BattleUpdateContext) {
		while (true) {
			val state = this.state
			if (state is BattleStateMachine.NextTurn && System.nanoTime() >= state.startAt) {
				val next = nextCombatantOnTurn(context)
				if (next != null) beginTurn(context, next)
			} else break
		}

		val state = this.state
		if (state is BattleStateMachine.Wait && System.nanoTime() > state.startTime + 250_000_000L) {
			this.state = BattleStateMachine.NextTurn(System.nanoTime() + 250_000_000L)
		}

		if (state is BattleStateMachine.MeleeAttack.MoveTo && state.finished) {
			this.state = BattleStateMachine.MeleeAttack.Strike(
				state.attacker, state.target,
				state.skill, state.reactionChallenge
			)
		}
		if (state is BattleStateMachine.MeleeAttack.Strike) {
			if (state.canDealDamage && !state.hasDealtDamage) {
				val passedChallenge = state.reactionChallenge?.wasPassed() ?: false
				val result = if (state.skill == null) MoveResultCalculator(context).computeBasicAttackResult(
					state.attacker, state.target, passedChallenge
				) else MoveResultCalculator(context).computeSkillResult(
					state.skill, state.attacker, listOf(state.target), passedChallenge
				)

				applyMoveResult(context, result, state.attacker)
				for (entry in result.targets) {
					if (!entry.missed && state.skill != null) {
						state.skill.particleEffect?.let { particles.add(ParticleEffectState(
							it, entry.target.getPosition(this),
							entry.target.isOnPlayerSide
						)) }
					}
				}
				state.hasDealtDamage = true
			}
			if (state.finished) {
				this.state = BattleStateMachine.MeleeAttack.JumpBack(
					state.attacker, state.target,
					state.skill, state.reactionChallenge
				)
			}
		}
		if (state is BattleStateMachine.MeleeAttack.JumpBack && state.finished) {
			this.state = BattleStateMachine.NextTurn(System.nanoTime() + 250_000_000L)
		}

		if (state is BattleStateMachine.CastSkill) {
			if (state.canDealDamage && state.targetParticlesSpawnTime == 0L) {
				val particleEffect = state.skill.particleEffect ?: throw UnsupportedOperationException(
					"Ranged skills must have a particle effect"
				)

				for (target in state.targets) {
					val particle = ParticleEffectState(
						particleEffect,
						target.getPosition(this),
						target.isOnPlayerSide
					)
					particle.startTime = System.nanoTime()
					particles.add(particle)
				}
				state.targetParticlesSpawnTime = System.nanoTime()
			}

			if (state.targetParticlesSpawnTime != 0L) {
				val spentSeconds = (System.nanoTime() - state.targetParticlesSpawnTime) / 1000_000_000f
				if (spentSeconds > state.skill.particleEffect!!.damageDelay) {
					val passedChallenge = state.reactionChallenge?.wasPassed() ?: false

					val result = MoveResultCalculator(context).computeSkillResult(
						state.skill, state.caster, state.targets, passedChallenge
					)

					applyMoveResult(context, result, state.caster)
					this.state = BattleStateMachine.NextTurn(System.nanoTime() + 750_000_000L)
				}
			}
		}

		if (state is BattleStateMachine.UseItem && state.canDrinkItem) {
			val result = MoveResultCalculator(context).computeItemResult(
				state.item, state.thrower, state.target
			)
			applyMoveResult(context, result, state.thrower)
			this.state = BattleStateMachine.NextTurn(System.nanoTime() + 500_000_000L)

			val particleEffect = state.item.consumable?.particleEffect
			if (particleEffect != null) {
				val particle = ParticleEffectState(
					particleEffect,
					state.target.getPosition(this),
					state.target.isOnPlayerSide
				)
				particle.startTime = System.nanoTime()
				particles.add(particle)
			}
		}
	}

	private fun applyMoveResult(context: BattleUpdateContext, result: MoveResult, attacker: CombatantState) {
		for (sound in result.sounds) context.soundQueue.insert(sound)

		for (entry in result.targets) {
			val target = entry.target
			if (!entry.missed) {
				if (entry.damage != 0 || entry.damageMana == 0) {
					target.lastDamageIndicator = DamageIndicatorHealth(
						oldHealth = target.currentHealth,
						oldMana = target.currentMana,
						gainedHealth = -entry.damage,
						element = result.element,
						overrideColor = result.overrideBlinkColor,
					)
				} else {
					target.lastDamageIndicator = DamageIndicatorMana(
						oldHealth = target.currentHealth,
						oldMana = target.currentMana,
						gainedMana = -entry.damageMana,
						element = result.element,
						overrideColor = result.overrideBlinkColor,
					)
				}

				target.currentHealth -= entry.damage
				target.currentMana -= entry.damageMana

				target.statusEffects.removeAll(entry.removedEffects)
				target.statusEffects.addAll(entry.addedEffects)
				for ((stat, modifier) in entry.addedStatModifiers) {
					target.statModifiers[stat] = target.statModifiers.getOrDefault(stat, 0) + modifier
				}
				target.clampHealthAndMana(context)
				if (target.isAlive() && target.currentHealth <= target.maxHealth / 5) {
					target.statusEffects.addAll(target.getSosEffects(context))
				}
			} else target.lastDamageIndicator = DamageIndicatorMiss(target.currentHealth, target.currentMana)
		}

		if (result.restoreAttackerHealth != 0) {
			attacker.lastDamageIndicator = DamageIndicatorHealth(
				oldHealth = attacker.currentHealth,
				oldMana = attacker.currentMana,
				gainedHealth = result.restoreAttackerHealth,
				element = result.element,
				overrideColor = 0,
			)
		} else if (result.restoreAttackerMana != 0) {
			attacker.lastDamageIndicator = DamageIndicatorMana(
				oldHealth = attacker.currentHealth,
				oldMana = attacker.currentMana,
				gainedMana = result.restoreAttackerMana,
				element = result.element,
				overrideColor = 0,
			)
		}
		attacker.currentHealth += result.restoreAttackerHealth
		attacker.currentMana += result.restoreAttackerMana
		attacker.clampHealthAndMana(context)
		if (attacker.isAlive() && attacker.currentHealth <= attacker.maxHealth / 5) {
			attacker.statusEffects.addAll(attacker.getSosEffects(context))
		}
	}
}
