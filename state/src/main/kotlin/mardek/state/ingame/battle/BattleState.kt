package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.battle.PartyLayout
import mardek.content.characters.PlayableCharacter
import mardek.input.InputKey
import mardek.state.ingame.CampaignState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@BitStruct(backwardCompatible = true)
class BattleState(
	@BitField(id = 0)
	val battle: Battle,

	@BitField(id = 1)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceField(stable = true, label = "playable characters")
	val players: Array<PlayableCharacter?>,

	@BitField(id = 2)
	val playerLayout: PartyLayout,

	campaignState: CampaignState,
) {

	fun allPlayers() = (0 until 4).filter { players[it] != null }.map {
		CombatantReference(isPlayer = true, index = it, this)
	}

	fun livingPlayers() = allPlayers().filter { it.isAlive() }

	fun livingEnemies() = (0 until 4).filter { enemyStates[it] != null }.map {
		CombatantReference(isPlayer = false, index = it, this)
	}

	@BitField(id = 3)
	@NestedFieldSetting(path = "c", optional = true)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	val enemies = battle.startingEnemies

	@BitField(id = 4)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val enemyStates = Array(4) { index ->
		val enemy = enemies[index] ?: return@Array null
		CombatantState(enemy)
	}

	@BitField(id = 5)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val playerStates = Array(4) { index ->
		val player = players[index] ?: return@Array null
		CombatantState(player, campaignState.characterStates[player]!!)
	}

	@BitField(id = 6, optional = true)
	@ReferenceField(stable = false, label = "combatants")
	var onTurn: CombatantReference? = null

	@BitField(id = 6) // TODO right annotation
	@ClassField(root = BattleMove::class)
	var currentMove: BattleMove = BattleMoveThinking(0.seconds)

	var selectedMove: BattleMoveSelection = BattleMoveSelectionAttack(target = null)

	@BitField(id = 8)
	var outcome = BattleOutcome.Busy
		private set

	val startTime = System.nanoTime()

	var updatedTime = 0.seconds
		private set

	@Suppress("unused")
	internal constructor() : this(Battle(), arrayOf(null, null, null, null), PartyLayout(), CampaignState())

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
		val combatants = livingPlayers() + livingEnemies()
		if (combatants.none { it.isPlayer }) outcome = BattleOutcome.GameOver
		if (combatants.none { !it.isPlayer }) outcome = BattleOutcome.Victory
		if (outcome != BattleOutcome.Busy) return

		val simulator = TurnOrderSimulator(this, context)
		if (simulator.checkReset()) {
			for (combatant in combatants) combatant.getState().spentTurnsThisRound = 0
		}
		beginTurn(context, simulator.next()!!)
	}

	private fun beginTurn(context: BattleUpdateContext, combatant: CombatantReference) {
		val combatantState = combatant.getState()
		combatantState.spentTurnsThisRound += 1
		combatantState.totalSpentTurns += 1
		// TODO Allow status effects to skip the turn
		onTurn = combatant

		currentMove = if (combatant.isPlayer) {
			context.soundQueue.insert(context.sounds.ui.partyScroll)
			selectedMove = BattleMoveSelectionAttack(target = null)
			BattleMoveThinking(updatedTime)
		} else {
			MonsterStrategyCalculator(this, context).determineNextMove()
		}
		println("currentMove is $currentMove")
	}

	fun update(context: BattleUpdateContext, timeStep: Duration) {
		updatedTime += timeStep
		while (onTurn == null && outcome == BattleOutcome.Busy) updateOnTurn(context)
		if (outcome != BattleOutcome.Busy) return

		val currentMove = this.currentMove
		if (currentMove is BattleMoveWait && updatedTime > currentMove.stateDecisionTime + 500.milliseconds) {
			onTurn = null
		}

		if (currentMove is BattleMoveBasicAttack) {
			if (currentMove.finishedStrike && !currentMove.processedStrike) {
				val attacker = onTurn!!
				val passedChallenge = true // TODO
				val result = MoveResultCalculator(this, context).computeBasicAttackResult(attacker, currentMove.target, passedChallenge)

				context.soundQueue.insert(result.sound)

				if (!result.missed) {
					val targetState = currentMove.target.getState()
					val attackerState = onTurn!!.getState()
					println("damage is ${result.damage}")
					targetState.currentHealth -= result.damage
					attackerState.currentHealth += result.restoreAttackerHealth
					attackerState.currentMana += result.restoreAttackerMana
					targetState.statusEffects.removeAll(result.removedEffects)
					targetState.statusEffects.addAll(result.addedEffects)
					for ((stat, modifier) in result.addedStatModifiers) {
						targetState.statModifiers[stat] = targetState.statModifiers.getOrDefault(stat, 0) + modifier
					}
					attackerState.clampHealthAndMana()
					targetState.clampHealthAndMana()

					if (attackerState.currentHealth > 0 && attackerState.currentHealth <= attackerState.maxHealth / 5) {
						attackerState.statusEffects.addAll(attacker.getSosEffects(context))
					}
					if (targetState.currentHealth > 0 && targetState.currentHealth <= targetState.maxHealth / 5) {
						targetState.statusEffects.addAll(currentMove.target.getSosEffects(context))
					}

					if (!attacker.isPlayer && attackerState.currentHealth == 0) {
						enemies[attacker.index] = null
						enemyStates[attacker.index] = null
					}
					if (!currentMove.target.isPlayer && targetState.currentHealth == 0) {
						enemies[currentMove.target.index] = null
						enemyStates[currentMove.target.index] = null
					}
				}

				currentMove.processedStrike = true
			}
			if (currentMove.finishedJump) {
				this.onTurn = null
			}
		}
	}
}
