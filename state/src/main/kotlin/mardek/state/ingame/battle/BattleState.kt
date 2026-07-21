package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.battle.Battle
import mardek.content.battle.PartyLayout
import mardek.content.characters.PlayableCharacter
import mardek.content.skill.ActiveSkill
import mardek.content.skill.ReactionSkillType
import mardek.input.InputKey
import mardek.input.MouseMoveEvent
import mardek.content.util.Time
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The state of an ongoing battle. This class tracks e.g. the health, mana, and status effects of all combatants, as
 * well as who is currently on turn.
 */
@BitStruct(backwardCompatible = true)
class BattleState(

	/**
	 * The battle 'configuration' from which this battle state started. This configuration determines the music,
	 * enemy layout, and the starting enemies.
	 */
	@BitField(id = 0)
	val battle: Battle,

	players: Array<PlayableCharacter?>,

	/**
	 * The positions of the player combatants, which is almost always the DEFAULT layout.
	 */
	@BitField(id = 1)
	val playerLayout: PartyLayout,

	context: BattleUpdateContext,
) {

	/**
	 * The combatants on the player/right side. When all these combatants are fainted, it is game over.
	 *
	 * This array will always have a length of 4, but it will contain `null`s when there are less than 4 players.
	 *
	 * Usually (always in vanilla MARDEK), this array contains only [PlayerCombatantState]s,
	 * but this is not required by the engine. This engine allows monsters to fight alongside the player.
	 */
	@BitField(id = 2)
	@ClassField(root = CombatantState::class)
	@ReferenceFieldTarget(label = "combatants")
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val players: Array<CombatantState?> = players.map { player ->
		if (player != null) PlayerCombatantState(player, context.characterStates[player]!!, true) else null
	}.toTypedArray()

	/**
	 * The combatants on the enemy/left side. When all these combatants are defeated, the player wins the battle.
	 *
	 * This array will always have a length of 4, but it will contain `null`s when there are less than 4 opponents.
	 * Furthermore, defeated combatants will be replaced with `null` once their fainting animation is finished.
	 */
	@BitField(id = 3)
	@ClassField(root = CombatantState::class)
	@ReferenceFieldTarget(label = "combatants")
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val opponents: Array<CombatantState?> = battle.startingEnemies.map { enemy ->
		if (enemy != null) MonsterCombatantState(
			enemy.monster, enemy.level, false, enemy.overrideDisplayName
		) else null
	}.toTypedArray()

	/**
	 * The state (machine) of this battle state. This field tracks which combatant is currently on turn, and what that
	 * combatant is doing.
	 */
	@BitField(id = 4)
	@ClassField(root = BattleStateMachine::class)
	var state: BattleStateMachine = BattleStateMachine.NextTurn(
		context.campaignTime, 750.milliseconds
	)

	/**
	 * The (campaign) time at which the battle started. This is currently only used to render the fade-in.
	 */
	@BitField(id = 5)
	val startTime = context.campaignTime

	/**
	 * The ongoing (standard) particle effects. This contains most of the particle effects, but not the ones tied to
	 * status effects or animations.
	 *
	 * This field is only used by the renderer.
	 */
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

	constructor() : this(Battle(), arrayOf(null, null, null, null), PartyLayout(), BattleUpdateContext())

	/**
	 * Geta a list containing all non-null [players]
	 */
	fun allPlayers() = players.filterNotNull()

	/**
	 * Gets a list containing all [players] with at least 1 HP
	 */
	fun livingPlayers() = allPlayers().filter { it.isAlive() }

	/**
	 * Gets a list containing all non-null [opponents]
	 */
	fun allOpponents() = opponents.filterNotNull()

	/**
	 * Gets a list containing all opponents with at least 1 HP.
	 *
	 * Note that this is usually equivalent to [allOpponents], since monsters 'vanish' after they are defeated.
	 */
	fun livingOpponents() = allOpponents().filter { it.isAlive() }

	internal fun confirmMove(context: BattleUpdateContext, newState: BattleStateMachine) {
		this.state = newState
		if (newState is BattleStateMachine.Wait) context.soundQueue.insert(context.sounds.ui.clickCancel)
	}

	/**
	 * Gets the active/visible reaction skill challenge, or `null` if there is no active/visible reaction
	 * skill challenge.
	 */
	fun getReactionChallenge(): ReactionChallenge? {
		return when (val state = this.state) {
			is BattleStateMachine.MeleeAttack -> state.reactionChallenge
			is BattleStateMachine.BreathAttack -> state.reactionChallenge
			is BattleStateMachine.CastSkill -> state.reactionChallenge
			else -> null
		}
	}

	/**
	 * This method should be called whenever an [mardek.input.InputKeyEvent] with `didPress = true` is fired during
	 * a battle. It should be called by [mardek.state.ingame.area.AreaState.processBattleKeyEvent].
	 */
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
		if (key == InputKey.Interact && reactionChallenge != null) {
			val wasPending = reactionChallenge.isPending(context.campaignTime)
			reactionChallenge.click(context.campaignTime)
			if (wasPending && !reactionChallenge.isPending(context.campaignTime) && !reactionChallenge.wasPassed()) {
				context.soundQueue.insert(context.sounds.ui.clickReject)
			}
		}

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

	/**
	 * This method should be called whenever a [MouseMoveEvent] is fired during a battle.
	 * It should be invoked by [mardek.state.ingame.area.AreaState.processMouseMove].
	 */
	fun processMouseMove(event: MouseMoveEvent) {
		this.lastMousePosition = Pair(event.newX, event.newY)
	}

	private fun nextCombatantOnTurn(context: BattleUpdateContext): CombatantState? {
		val combatants = livingPlayers() + livingOpponents()
		if (combatants.none { it.isOnPlayerSide }) state = BattleStateMachine.GameOver(context.campaignTime)
		if (combatants.none { !it.isOnPlayerSide }) {
			state = BattleStateMachine.Victory(context.campaignTime)
			context.statistics.battlesWon += 1
			for (combatant in combatants) {
				combatant.incrementPassiveSkillsMastery(context)
				combatant.getPerformance(context).numBattles += 1
			}
		}
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
		if (effects.removedEffects.isNotEmpty()) {
			effects.combatant.statusEffects.removeAll(effects.removedEffects)
			for (effect in effects.removedEffects) {
				effects.combatant.renderInfo.effectHistory.remove(effect)
			}
			effects.removedEffects.clear()
		}

		if (effects.takeDamage.isNotEmpty()) {
			if (context.campaignTime.virtual >= effects.applyNextDamageAt.virtual) {
				val takeDamage = effects.takeDamage.removeFirst()
				val dpt = takeDamage.effect.damagePerTurn!!
				effects.applyNextDamageAt = context.campaignTime + BattleStateMachine.NextTurnEffects.DAMAGE_DELAY

				val oldHealth = effects.combatant.currentHealth
				effects.combatant.currentHealth -= takeDamage.amount
				effects.combatant.clampHealthAndMana(context)

				if (effects.combatant.currentHealth != oldHealth) {
					if (effects.combatant.currentHealth < oldHealth) {
						effects.combatant.getPerformance(context).damageReceived += oldHealth - effects.combatant.currentHealth
					}
					effects.combatant.renderInfo.lastDamageIndicator = DamageIndicatorHealth(
						oldHealth = oldHealth, time = context.campaignTime, gainedHealth = -takeDamage.amount,
						element = dpt.element, overrideColor = dpt.blinkColor,
					)
					val particle = ParticleEffectState(
						particle = dpt.particleEffect,
						position = effects.combatant.renderInfo.statusEffectPoint,
						mirrorX = effects.combatant.isOnPlayerSide,
					)
					particles.add(particle)
					if (!effects.combatant.isAlive()) {
						effects.combatant.getPerformance(context).numFaints += 1
						if (!effects.combatant.isOnPlayerSide) context.statistics.numKills += 1
						state = BattleStateMachine.NextTurn(context.campaignTime, 1.seconds)
						if (!effects.combatant.isOnPlayerSide && effects.combatant is MonsterCombatantState) {
							context.encyclopedia.reportMonsterAsSlain(effects.combatant.monster)
							for (player in livingPlayers()) {
								player.gainExperience(
									context, effects.combatant.monster.experience *
											effects.combatant.getLevel(context)
								)
							}
						}
					}
				}
			}
			return
		}

		val forceMove = effects.forceMove
		if (forceMove != null && context.campaignTime.virtual < effects.applyNextDamageAt.virtual) return

		state = if (forceMove != null) {
			if (forceMove.blinkColor != 0) {
				effects.combatant.renderInfo.lastForcedTurn = ForcedTurnBlink(forceMove.blinkColor, context.campaignTime)
			}
			val particleEffect = forceMove.particleEffect
			if (particleEffect != null) {
				val particle = ParticleEffectState(
					particle = particleEffect,
					position = effects.combatant.renderInfo.statusEffectPoint,
					mirrorX = effects.combatant.isOnPlayerSide,
				)
				particles.add(particle)
			}
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
	 * This method should be called during every call to [mardek.state.ingame.InGameState.update] during this
	 * battle. It should be invoked by [mardek.state.ingame.area.AreaState.updateActiveBattle].
	 */
	fun update(context: BattleUpdateContext) {
		while (true) {
			val state = this.state
			if (state is BattleStateMachine.NextTurn && context.campaignTime.virtualOffset(state.lastFinishTime) >= state.delay) {
				val next = nextCombatantOnTurn(context)
				if (next != null) beginTurn(context, next)
			} else break
		}

		val state = this.state
		if (state is BattleStateMachine.Wait && context.campaignTime.virtualOffset(state.startTime) >= 250.milliseconds) {
			this.state = BattleStateMachine.NextTurn(context.campaignTime, 250.milliseconds)
		}

		if (state is BattleStateMachine.NextTurnEffects) prepareNextTurn(context, state)

		if (state is BattleStateMachine.MeleeAttack.MoveTo && state.finished) {
			this.state = BattleStateMachine.MeleeAttack.Strike(
				state.attacker, state.target, state.skill, state.reactionChallenge,
				context.campaignTime,
			)
		}
		if (state is BattleStateMachine.MeleeAttack.Strike) {
			if (state.canDealDamage && !state.hasDealtDamage && !state.isReactionChallengePending(context.campaignTime)) {
				if (state.skill != null) state.attacker.incrementActiveSkillMastery(context, state.skill)
				val passedChallenge = state.reactionChallenge?.wasPassed() ?: false
				val result = if (state.skill == null) MoveResultCalculator(context).computeBasicAttackResult(
					state.attacker, state.target, passedChallenge
				) else MoveResultCalculator(context).computeSkillResult(
					state.skill, state.attacker, arrayOf(state.target), passedChallenge
				)

				state.attacker.getPerformance(context).numMeleeAttacks += 1
				if (passedChallenge) {
					state.attacker.incrementReactionSkillsMastery(context, ReactionSkillType.MeleeAttack)
					state.target.incrementReactionSkillsMastery(context, ReactionSkillType.MeleeDefense)
				}

				applyMoveResultEntirely(context, result, state.attacker, state.skill, false)
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
					state.attacker, state.target, state.skill, state.reactionChallenge,
					context.campaignTime,
				)
			}
		}
		if (state is BattleStateMachine.MeleeAttack.JumpBack && state.finished) {
			this.state = BattleStateMachine.NextTurn(context.campaignTime, 250.milliseconds)
		}

		if (state is BattleStateMachine.BreathAttack.MoveTo && state.finished) {
			this.state = BattleStateMachine.BreathAttack.Attack(
				state.attacker, state.targets, state.skill,
				state.reactionChallenge, context.campaignTime
			)
		}
		if (state is BattleStateMachine.BreathAttack.Attack) {
			if (state.canDealDamage && !state.hasDealtDamage && !state.isReactionChallengePending(context.campaignTime)) {
				state.attacker.incrementActiveSkillMastery(context, state.skill)
				val passedChallenge = state.reactionChallenge?.wasPassed() ?: false
				val result = MoveResultCalculator(context).computeSkillResult(
					state.skill, state.attacker, state.targets, passedChallenge
				)

				state.attacker.getPerformance(context).numMagicSkills += 1
				if (passedChallenge) {
					state.attacker.incrementReactionSkillsMastery(context, ReactionSkillType.RangedAttack)
					for (target in state.targets) {
						target.incrementReactionSkillsMastery(context, ReactionSkillType.RangedDefense)
					}
				}

				applyMoveResultEntirely(context, result, state.attacker, state.skill, false)
				state.skill.particleEffect?.let {
					particles.add(ParticleEffectState(
						particle = it,
						position = state.attacker.renderInfo.activeBreathSource,
						mirrorX = state.attacker.isOnPlayerSide,
					))
				}
				state.hasDealtDamage = true
			}
			if (state.finished && state.hasDealtDamage) {
				this.state = BattleStateMachine.BreathAttack.JumpBack(
					state.attacker, state.targets, state.skill,
					state.reactionChallenge, context.campaignTime,
				)
			}
		}
		if (state is BattleStateMachine.BreathAttack.JumpBack && state.finished) {
			this.state = BattleStateMachine.NextTurn(context.campaignTime, 250.milliseconds)
		}

		if (state is BattleStateMachine.CastSkill) {
			if (!state.hasFinishedCastingAnimation) {
				val particlePositions = state.caster.renderInfo.castingParticlePositions
				val particleEffect = state.skill.element.spellCastEffect
				val castingParticlePeriod = 1.seconds / 30
				if (context.campaignTime.virtualOffset(state.lastCastParticleSpawnTime) >= castingParticlePeriod &&
					particlePositions.isNotEmpty() && particleEffect != null
				) {
					state.lastCastParticleSpawnTime = context.campaignTime
					for (position in particlePositions) {
						particles.add(ParticleEffectState(
							particle = particleEffect,
							position = position,
							mirrorX = true,
						))
					}
				}
			}

			if (state.canSpawnTargetParticles && state.targetParticlesSpawnTime == Time.ZERO && !state.hasAppliedAllDamage()) {
				val particleEffect = state.skill.particleEffect
				if (particleEffect != null) {
					for ((index, target) in state.targets.withIndex()) {
						val particle = ParticleEffectState(
							particle = particleEffect,
							position = target.renderInfo.hitPoint,
							mirrorX = true,
						)
						particle.startTime = context.campaignTime + 250.milliseconds * index
						particles.add(particle)
					}
				}

				state.targetParticlesSpawnTime = context.campaignTime
			}

			val damageDelay = state.skill.particleEffect?.damageDelay ?: Duration.ZERO
			if (state.targetParticlesSpawnTime != Time.ZERO && !state.isReactionChallengePending(context.campaignTime) &&
				state.calculatedDamage == null
			) {
				val elapsedTime = context.campaignTime.virtualOffset(state.targetParticlesSpawnTime)
				if (elapsedTime > damageDelay) {
					val passedChallenge = state.reactionChallenge?.wasPassed() ?: false

					val result = MoveResultCalculator(context).computeSkillResult(
						state.skill, state.caster, state.targets, passedChallenge
					)

					state.caster.getPerformance(context).numMagicSkills += 1
					if (passedChallenge) {
						state.caster.incrementReactionSkillsMastery(context, ReactionSkillType.RangedAttack)
						for (target in state.targets) {
							target.incrementReactionSkillsMastery(context, ReactionSkillType.RangedDefense)
						}
					}

					applyMoveResultToAttacker(context, result, state.caster, state.skill, false)
					state.calculatedDamage = state.targets.mapIndexed { index, target ->
						if (target != result.targets[index].target) {
							throw Error("Target mismatch")
						}
						result.targets[index]
					}.toTypedArray()
					state.caster.incrementActiveSkillMastery(context, state.skill)
				}
			}

			val calculatedDamage = state.calculatedDamage

			if (calculatedDamage != null) {
				for ((index, targetDamage) in calculatedDamage.withIndex()) {
					if (targetDamage == null) continue

					val elapsedTime = context.campaignTime.virtualOffset(state.targetParticlesSpawnTime)
					if (elapsedTime > damageDelay + 250.milliseconds * index) {
						applyMoveResultToTarget(context, targetDamage, state.caster)
						calculatedDamage[index] = null
					}
				}
			}

			if (state.hasFinishedCastingAnimation && state.hasAppliedAllDamage()) {
				this.state = BattleStateMachine.NextTurn(context.campaignTime, 500.milliseconds)
			}
		}

		if (state is BattleStateMachine.UseItem && state.canDrinkItem) {
			val result = MoveResultCalculator(context).computeItemResult(
				state.item, state.thrower, state.target
			)
			state.thrower.getPerformance(context).numItems += 1
			context.statistics.itemsConsumed += 1
			applyMoveResultEntirely(context, result, state.thrower, null, true)
			this.state = BattleStateMachine.NextTurn(context.campaignTime, 500.milliseconds)

			val particleEffect = state.item.consumable?.particleEffect
			if (particleEffect != null) {
				val particle = ParticleEffectState(
					particle = particleEffect,
					position = state.target.renderInfo.hitPoint,
					mirrorX = true,
				)
				particles.add(particle)
			}
		}
	}

	private fun applyMoveResultEntirely(
		context: BattleUpdateContext, result: MoveResult,
		attacker: CombatantState, skill: ActiveSkill?, isConsumable: Boolean,
	) {
		applyMoveResultToAttacker(context, result, attacker, skill, isConsumable)
		for (targetEntry in result.targets) applyMoveResultToTarget(context, targetEntry, attacker)
	}

	private fun applyMoveResultToTarget(
		context: BattleUpdateContext, entry: MoveResult.Entry, attacker: CombatantState
	) {
		val target = entry.target
		if (!entry.missed) {

			// We can only show 1 damage indicator (otherwise, they overlap), so we should choose the best one
			// 1. If mana damage was dealt, but no health damage, we show the mana damage
			if (entry.damageMana != 0 && entry.damage == 0) {
				target.renderInfo.lastDamageIndicator = DamageIndicatorMana(
					oldHealth = target.currentHealth,
					time = context.campaignTime,
					gainedMana = -entry.damageMana,
					element = entry.element,
					overrideColor = entry.overrideBlinkColor,
				)
			} else {
				val isNotSpecial = entry.addedEffects.isEmpty() && entry.removedEffects.isEmpty() &&
						entry.addedStatModifiers.isEmpty()

				// 2. If health damage was dealt, we always show the health damage
				// 3. If no health damage was dealt, but something else *did* happen, we show nothing here.
				//    In such cases, this other effect will be visualized by another part of the renderer.
				// 4. If no health damage was dealt, but nothing else happened either, it was probably an attack that
				//    was too weak to deal any damage. In such cases, we explicitly show that it did 0 damage.
				if (entry.damage != 0 || isNotSpecial) {
					target.renderInfo.lastDamageIndicator = DamageIndicatorHealth(
						oldHealth = target.currentHealth,
						time = context.campaignTime,
						gainedHealth = -entry.damage,
						element = entry.element,
						overrideColor = entry.overrideBlinkColor,
					)
				}
			}

			val oldHealth = target.currentHealth
			target.currentHealth -= entry.damage
			target.currentMana -= entry.damageMana

			target.statusEffects.addAll(entry.addedEffects)
			for ((stat, modifier) in entry.addedStatModifiers) {
				target.statModifiers[stat] = target.statModifiers.getOrDefault(stat, 0) + modifier
			}
			target.clampHealthAndMana(context)

			val realDamage = oldHealth - target.currentHealth
			if (realDamage > 0) {
				target.getPerformance(context).damageReceived += realDamage
				attacker.getPerformance(context).damageDealt += realDamage
			} else {
				attacker.getPerformance(context).damageDealt -= realDamage
			}

			if (target.isAlive()) {
				target.statusEffects.removeAll(entry.removedEffects)
				for (effect in entry.removedEffects) target.renderInfo.effectHistory.remove(effect)
				for (effect in entry.addedEffects) target.renderInfo.effectHistory.add(effect)
			} else {
				attacker.getPerformance(context).numKills += 1
				target.getPerformance(context).numFaints += 1
				if (!target.isOnPlayerSide) context.statistics.numKills += 1
				if (target is MonsterCombatantState) {
					attacker.gainExperience(context, target.monster.experience * target.getLevel(context))
					context.encyclopedia.reportMonsterAsSlain(target.monster)
					for (player in livingPlayers()) {
						if (player !== attacker && target.isOnPlayerSide != player.isOnPlayerSide) {
							player.gainExperience(
								context, target.monster.experience * target.getLevel(context) / 2
							)
						}
					}
				}
			}
		} else {
			target.renderInfo.lastDamageIndicator = DamageIndicatorMiss(
				target.currentHealth, context.campaignTime
			)
		}
	}

	private fun applyMoveResultToAttacker(
		context: BattleUpdateContext, result: MoveResult,
		attacker: CombatantState, skill: ActiveSkill?, isConsumable: Boolean,
	) {
		for (sound in result.sounds) context.soundQueue.insert(sound)
		if (result.restoreAttackerHealth != 0) {
			attacker.renderInfo.lastDamageIndicator = DamageIndicatorHealth(
				oldHealth = attacker.currentHealth,
				time = context.campaignTime,
				gainedHealth = result.restoreAttackerHealth,
				element = result.element,
				overrideColor = 0,
			)
		} else if (result.restoreAttackerMana != 0) {
			attacker.renderInfo.lastDamageIndicator = DamageIndicatorMana(
				oldHealth = attacker.currentHealth,
				time = context.campaignTime,
				gainedMana = result.restoreAttackerMana,
				element = result.element,
				overrideColor = 0,
			)
		}
		attacker.currentHealth += result.restoreAttackerHealth
		if (result.restoreAttackerHealth < 0) {
			attacker.getPerformance(context).damageReceived -= result.restoreAttackerHealth
		}
		attacker.currentMana += result.restoreAttackerMana
		attacker.clampHealthAndMana(context)
		if (attacker.isAlive() && attacker.currentHealth <= attacker.maxHealth / 5) {
			attacker.statusEffects.addAll(attacker.getSosEffects(context))
		}

		val notMissedTargets = result.targets.filter { !it.missed }
		if (!isConsumable && notMissedTargets.isNotEmpty()) {
			var gainedExperience = 100 * notMissedTargets.maxOf { it.target.getLevel(context) }
			if (skill != null && skill.isMelee) gainedExperience *= 2
			attacker.gainExperience(context, gainedExperience)
		}
	}
}
