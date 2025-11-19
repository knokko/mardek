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

	val customParticles = mutableListOf<CustomParticle>()

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
		return when (val state = this.state) {
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
		if (key == InputKey.Interact && reactionChallenge != null) reactionChallenge.click()

		if (key == InputKey.Click) {
			val mouse = this.lastMousePosition
			for (combatant in allPlayers() + allOpponents()) {
				val renderRegion = combatant.renderInfo.renderedInfoBlock
				if (mouse != null && renderRegion != null && renderRegion.contains(mouse.first, mouse.second)) {
					this.openCombatantInfo = combatant
					break
				}
			}
			if (this.openCombatantInfo != openCombatantInfo && this.openCombatantInfo != null) {
				context.soundQueue.insert(context.sounds.ui.clickConfirm)
			}
			if (this.openCombatantInfo == openCombatantInfo && openCombatantInfo != null) {
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

		state = computeStatusEffectsBeforeTurn(combatant, context)
	}

	private fun prepareNextTurn(context: BattleUpdateContext, effects: BattleStateMachine.NextTurnEffects) {
		val time = System.nanoTime()
		if (effects.removedEffects.isNotEmpty()) {
			effects.combatant.statusEffects.removeAll(effects.removedEffects)
			for (effect in effects.removedEffects) {
				effects.combatant.renderInfo.effectHistory.remove(effect, time)
			}
			effects.removedEffects.clear()
		}

		if (effects.takeDamage.isNotEmpty()) {
			if (time >= effects.applyNextDamageAt) {
				val takeDamage = effects.takeDamage.removeFirst()
				val dpt = takeDamage.effect.damagePerTurn!!
				effects.applyNextDamageAt = time + BattleStateMachine.NextTurnEffects.DAMAGE_DELAY

				val oldHealth = effects.combatant.currentHealth
				effects.combatant.currentHealth -= takeDamage.amount
				effects.combatant.clampHealthAndMana(context)

				if (effects.combatant.currentHealth != oldHealth) {
					effects.combatant.renderInfo.lastDamageIndicator = DamageIndicatorHealth(
						oldHealth = oldHealth, oldMana = effects.combatant.currentMana,
						gainedHealth = -takeDamage.amount, element = dpt.element, overrideColor = dpt.blinkColor
					)
					val particle = ParticleEffectState(
						particle = dpt.particleEffect,
						position = effects.combatant.renderInfo.statusEffectPoint,
						mirrorX = effects.combatant.isOnPlayerSide,
					)
					particle.startTime = System.nanoTime()
					particles.add(particle)
					if (!effects.combatant.isAlive()) state = BattleStateMachine.NextTurn(time + 1000_000_000L)
				}
			}
			return
		}

		val forceMove = effects.forceMove
		if (forceMove != null && time < effects.applyNextDamageAt) return

		state = if (forceMove != null) {
			if (forceMove.blinkColor != 0) effects.combatant.renderInfo.lastForcedTurn = ForcedTurnBlink(forceMove.blinkColor)
			val particleEffect = forceMove.particleEffect
			if (particleEffect != null) {
				val particle = ParticleEffectState(
					particle = particleEffect,
					position = effects.combatant.renderInfo.statusEffectPoint,
					mirrorX = effects.combatant.isOnPlayerSide,
				)
				particle.startTime = System.nanoTime()
				particles.add(particle)
			}
			forceMove.move.refreshStartTime()
			forceMove.move as BattleStateMachine
		} else if (effects.combatant is PlayerCombatantState) {
			context.soundQueue.insert(context.sounds.ui.scroll2)
			BattleStateMachine.SelectMove(effects.combatant)
		} else {
			MonsterStrategyCalculator(
				this, effects.combatant as MonsterCombatantState, context
			).determineNextMove() as BattleStateMachine
		}
	}

	/**
	 * This method should be called when the player loads a save that was in a battle.
	 *
	 * This method will potentially reset the start of some animations.
	 */
	fun markSessionStart() {
		val state = this.state
		if (state is BattleStateMachine.Move) state.refreshStartTime()
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

		if (state is BattleStateMachine.NextTurnEffects) prepareNextTurn(context, state)

		if (state is BattleStateMachine.MeleeAttack.MoveTo && state.finished) {
			this.state = BattleStateMachine.MeleeAttack.Strike(
				state.attacker, state.target,
				state.skill, state.reactionChallenge
			)
		}
		if (state is BattleStateMachine.MeleeAttack.Strike) {
			if (state.canDealDamage && !state.hasDealtDamage && !state.isReactionChallengePending()) {
				val passedChallenge = state.reactionChallenge?.wasPassed() ?: false
				val result = if (state.skill == null) MoveResultCalculator(context).computeBasicAttackResult(
					state.attacker, state.target, passedChallenge
				) else MoveResultCalculator(context).computeSkillResult(
					state.skill, state.attacker, arrayOf(state.target), passedChallenge
				)

				applyMoveResultEntirely(context, result, state.attacker)
				for (entry in result.targets) {
					if (!entry.missed && state.skill != null) {
						state.skill.particleEffect?.let { particles.add(ParticleEffectState(
							particle = it,
							position = entry.target.renderInfo.hitPoint,
							mirrorX = true,
						)) }
					}
				}
				state.hasDealtDamage = true
			}
			if (state.finished && state.hasDealtDamage) {
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
			if (!state.hasFinishedCastingAnimation) {
				val particlePositions = state.caster.renderInfo.castingParticlePositions
				val particleEffect = state.skill.element.spellCastEffect
				val particleTime = System.nanoTime()
				if (particleTime > state.lastCastParticleSpawnTime + 1000_000_000L / 90 &&
					particlePositions.isNotEmpty() && particleEffect != null
				) {
					state.lastCastParticleSpawnTime = particleTime
					for (position in particlePositions) {
						particles.add(ParticleEffectState(
							particle = particleEffect,
							position = position,
							mirrorX = true,
						))
					}
				}
			}

			if (state.canSpawnTargetParticles && state.targetParticlesSpawnTime == 0L && !state.hasAppliedAllDamage()) {
				val particleEffect = state.skill.particleEffect ?: throw UnsupportedOperationException(
					"Ranged skills must have a particle effect"
				)

				for ((index, target) in state.targets.withIndex()) {
					val particle = ParticleEffectState(
						particle = particleEffect,
						position = target.renderInfo.hitPoint,
						mirrorX = true,
					)
					particle.startTime = System.nanoTime() + 250_000_000L * index
					particles.add(particle)
				}
				state.targetParticlesSpawnTime = System.nanoTime()
			}

			if (state.targetParticlesSpawnTime != 0L && !state.isReactionChallengePending() && state.calculatedDamage == null) {
				val spentSeconds = (System.nanoTime() - state.targetParticlesSpawnTime) / 1000_000_000f
				if (spentSeconds > state.skill.particleEffect!!.damageDelay) {
					val passedChallenge = state.reactionChallenge?.wasPassed() ?: false

					val result = MoveResultCalculator(context).computeSkillResult(
						state.skill, state.caster, state.targets, passedChallenge
					)
					applyMoveResultToAttacker(context, result, state.caster)
					state.calculatedDamage = state.targets.mapIndexed { index, target ->
						if (target != result.targets[index].target) {
							throw Error("Target mismatch")
						}
						result.targets[index]
					}.toTypedArray()
				}
			}
			val calculatedDamage = state.calculatedDamage

			if (calculatedDamage != null) {
				for ((index, targetDamage) in calculatedDamage.withIndex()) {
					if (targetDamage == null) continue
					val spentSeconds = (System.nanoTime() - state.targetParticlesSpawnTime) / 1000_000_000f
					if (spentSeconds > state.skill.particleEffect!!.damageDelay + 0.25 * index) {
						applyMoveResultToTarget(context, targetDamage)
						calculatedDamage[index] = null
					}
				}
			}

			if (state.hasFinishedCastingAnimation && state.hasAppliedAllDamage()) {
				this.state = BattleStateMachine.NextTurn(System.nanoTime() + 500_000_000L)
			}
		}

		if (state is BattleStateMachine.UseItem && state.canDrinkItem) {
			val result = MoveResultCalculator(context).computeItemResult(
				state.item, state.thrower, state.target
			)
			applyMoveResultEntirely(context, result, state.thrower)
			this.state = BattleStateMachine.NextTurn(System.nanoTime() + 500_000_000L)

			val particleEffect = state.item.consumable?.particleEffect
			if (particleEffect != null) {
				val particle = ParticleEffectState(
					particle = particleEffect,
					position = state.target.renderInfo.hitPoint,
					mirrorX = true,
				)
				particle.startTime = System.nanoTime()
				particles.add(particle)
			}
		}
	}

	private fun applyMoveResultEntirely(context: BattleUpdateContext, result: MoveResult, attacker: CombatantState) {
		applyMoveResultToAttacker(context, result, attacker)
		for (targetEntry in result.targets) applyMoveResultToTarget(context, targetEntry)
	}

	private fun applyMoveResultToTarget(context: BattleUpdateContext, entry: MoveResult.Entry) {
		val currentTime = System.nanoTime()

		val target = entry.target
		if (!entry.missed) {
			if (entry.damage != 0 || entry.damageMana == 0) {
				target.renderInfo.lastDamageIndicator = DamageIndicatorHealth(
					oldHealth = target.currentHealth,
					oldMana = target.currentMana,
					gainedHealth = -entry.damage,
					element = entry.element,
					overrideColor = entry.overrideBlinkColor,
				)
			} else {
				target.renderInfo.lastDamageIndicator = DamageIndicatorMana(
					oldHealth = target.currentHealth,
					oldMana = target.currentMana,
					gainedMana = -entry.damageMana,
					element = entry.element,
					overrideColor = entry.overrideBlinkColor,
				)
			}

			target.currentHealth -= entry.damage
			target.currentMana -= entry.damageMana

			target.statusEffects.addAll(entry.addedEffects)
			for ((stat, modifier) in entry.addedStatModifiers) {
				target.statModifiers[stat] = target.statModifiers.getOrDefault(stat, 0) + modifier
			}
			target.clampHealthAndMana(context)

			if (target.isAlive()) {
				target.statusEffects.removeAll(entry.removedEffects)
				for (effect in entry.removedEffects) target.renderInfo.effectHistory.remove(effect, currentTime)
				for (effect in entry.addedEffects) target.renderInfo.effectHistory.add(effect, currentTime)
			}
		} else target.renderInfo.lastDamageIndicator = DamageIndicatorMiss(target.currentHealth, target.currentMana)
	}

	private fun applyMoveResultToAttacker(context: BattleUpdateContext, result: MoveResult, attacker: CombatantState) {
		for (sound in result.sounds) context.soundQueue.insert(sound)
		if (result.restoreAttackerHealth != 0) {
			attacker.renderInfo.lastDamageIndicator = DamageIndicatorHealth(
				oldHealth = attacker.currentHealth,
				oldMana = attacker.currentMana,
				gainedHealth = result.restoreAttackerHealth,
				element = result.element,
				overrideColor = 0,
			)
		} else if (result.restoreAttackerMana != 0) {
			attacker.renderInfo.lastDamageIndicator = DamageIndicatorMana(
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
