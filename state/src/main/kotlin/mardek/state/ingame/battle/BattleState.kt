package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.battle.PartyLayout
import mardek.content.characters.PlayableCharacter
import mardek.content.skill.ActiveSkillMode
import mardek.input.InputKey
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

	@BitField(id = 4, optional = true)
	@ReferenceField(stable = false, label = "combatants")
	var onTurn: CombatantState? = null

	@BitField(id = 5)
	@ClassField(root = BattleMove::class)
	var currentMove: BattleMove = BattleMoveThinking()

	var selectedMove: BattleMoveSelection = BattleMoveSelectionAttack(target = null)

	@BitField(id = 6)
	var outcome = BattleOutcome.Busy
		private set

	val startTime = System.nanoTime()

	val particles = mutableListOf<ParticleEffectState>()

	@Suppress("unused")
	internal constructor() : this(Battle(), arrayOf(null, null, null, null), PartyLayout(), BattleUpdateContext())

	fun allPlayers() = players.filterNotNull()

	fun livingPlayers() = allPlayers().filter { it.isAlive() }

	fun allOpponents() = opponents.filterNotNull()

	fun livingOpponents() = allOpponents().filter { it.isAlive() }

	internal fun confirmMove(context: BattleUpdateContext, chosenMove: BattleMove) {
		this.selectedMove = BattleMoveSelectionAttack(target = null)
		this.currentMove = chosenMove
		if (currentMove is BattleMoveWait) context.soundQueue.insert(context.sounds.ui.clickCancel)
		else context.soundQueue.insert(context.sounds.ui.clickConfirm)
	}

	internal fun runAway() {
		outcome = BattleOutcome.RanAway
	}

	fun processKeyPress(key: InputKey, context: BattleUpdateContext) {
		val onTurn = this.onTurn
		if (onTurn != null && currentMove is BattleMoveThinking) {
			if (key == InputKey.Cancel) battleCancel(this, context)
			if (key == InputKey.Interact) battleClick(this, context)
			if (key == InputKey.MoveLeft || key == InputKey.MoveRight) battleScrollHorizontally(this, key, context)
			if (key == InputKey.MoveUp || key == InputKey.MoveDown) battleScrollVertically(this, key, context)
		}
	}

	private fun updateOnTurn(context: BattleUpdateContext) {
		val combatants = livingPlayers() + livingOpponents()
		if (combatants.none { it.isOnPlayerSide }) outcome = BattleOutcome.GameOver
		if (combatants.none { !it.isOnPlayerSide }) outcome = BattleOutcome.Victory
		if (outcome != BattleOutcome.Busy) return

		val simulator = TurnOrderSimulator(this, context)
		if (simulator.checkReset()) {
			for (combatant in combatants) combatant.spentTurnsThisRound = 0
		}
		beginTurn(context, simulator.next()!!)
	}

	private fun beginTurn(context: BattleUpdateContext, combatant: CombatantState) {
		combatant.spentTurnsThisRound += 1
		if (combatant is MonsterCombatantState) combatant.totalSpentTurns += 1
		// TODO Allow status effects to skip the turn, or to deal damage
		onTurn = combatant

		currentMove = if (combatant is PlayerCombatantState) {
			context.soundQueue.insert(context.sounds.ui.partyScroll)
			selectedMove = BattleMoveSelectionAttack(target = null)
			BattleMoveThinking()
		} else {
			MonsterStrategyCalculator(this, context).determineNextMove()
		}
		println("currentMove is $currentMove")
	}

	fun update(context: BattleUpdateContext) {
		while (onTurn == null && outcome == BattleOutcome.Busy) updateOnTurn(context)
		if (outcome != BattleOutcome.Busy) return

		val currentMove = this.currentMove
		if (currentMove is BattleMoveWait && System.nanoTime() > currentMove.decisionTime + 500_000_000L) {
			onTurn = null
		}

		if (currentMove is BattleMoveBasicAttack) {
			if (currentMove.finishedStrike && !currentMove.processedStrike) {
				val attacker = onTurn!!
				val passedChallenge = false // TODO
				val result = MoveResultCalculator(this, context).computeBasicAttackResult(
					attacker, currentMove.target, passedChallenge
				)

				applyMoveResult(context, result, attacker, currentMove.target)
				currentMove.processedStrike = true
			}
			if (currentMove.finishedJump) {
				this.onTurn = null
			}
		}

		if (currentMove is BattleMoveSkill) {
			if (currentMove.skill.mode == ActiveSkillMode.Melee) {
				if (currentMove.canProcessDamage && !currentMove.hasProcessedDamage) {
					val attacker = onTurn!!
					if (currentMove.target !is BattleSkillTargetSingle) throw UnsupportedOperationException(
						"Melee skills can only hit 1 target, but got ${currentMove.target}"
					)
					val target = currentMove.target.target
					val passedChallenge = false // TODO

					val result = MoveResultCalculator(this, context).computeMeleeSkillResult(
						currentMove.skill, attacker, target, passedChallenge
					)

					applyMoveResult(context, result, attacker, target)
					if (!result.missed) {
						currentMove.skill.particleEffect?.let { particles.add(ParticleEffectState(it, target)) }
					}
					currentMove.hasProcessedDamage = true
				}
			} else {
				println("WARNING: ${currentMove.skill} is not yet supported")
			}

			if (currentMove.finished) {
				this.onTurn = null
			}
		}
	}

	private fun applyMoveResult(
		context: BattleUpdateContext, result: MoveResult,
		attacker: CombatantState, target: CombatantState,
	) {
		if (result.sound != null) context.soundQueue.insert(result.sound)

		if (!result.missed) {
			target.lastDamageIndicator = DamageIndicatorHealth(
				oldHealth = target.currentHealth,
				oldMana = target.currentMana,
				gainedHealth = -result.damage,
				element = result.element,
			)
			if (result.restoreAttackerHealth != 0) {
				attacker.lastDamageIndicator = DamageIndicatorHealth(
					oldHealth = attacker.currentHealth,
					oldMana = attacker.currentMana,
					gainedHealth = result.restoreAttackerHealth,
					element = result.element,
				)
			} else if (result.restoreAttackerMana != 0) {
				attacker.lastDamageIndicator = DamageIndicatorMana(
					oldHealth = attacker.currentHealth,
					oldMana = attacker.currentMana,
					gainedMana = result.restoreAttackerMana,
					element = result.element,
				)
			}

			target.currentHealth -= result.damage
			attacker.currentHealth += result.restoreAttackerHealth
			attacker.currentMana += result.restoreAttackerMana
			target.statusEffects.removeAll(result.removedEffects)
			target.statusEffects.addAll(result.addedEffects)
			for ((stat, modifier) in result.addedStatModifiers) {
				target.statModifiers[stat] = target.statModifiers.getOrDefault(stat, 0) + modifier
			}
			attacker.clampHealthAndMana(context)
			target.clampHealthAndMana(context)

			if (attacker.isAlive() && attacker.currentHealth <= attacker.maxHealth / 5) {
				attacker.statusEffects.addAll(attacker.getSosEffects(context))
			}
			if (target.isAlive() && target.currentHealth <= target.maxHealth / 5) {
				target.statusEffects.addAll(target.getSosEffects(context))
			}
		} else target.lastDamageIndicator = DamageIndicatorMiss(target.currentHealth, target.currentMana)
	}
}
