package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CombatStat
import mardek.input.InputKey
import mardek.state.SoundQueue
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

	campaignState: CampaignState,
) {

	val allPossibleCombatants = (0 until 4).flatMap { listOf(
		CombatantReference(false, it, this), CombatantReference(true, it, this)
	) }

	@BitField(id = 2)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val enemyStates = Array(4) { index ->
		val enemy = battle.enemies[index] ?: return@Array null
		CombatantState(enemy)
	}

	@BitField(id = 3)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val playerStates = Array(4) { index ->
		val player = players[index] ?: return@Array null
		CombatantState(player, campaignState.characterStates[player]!!)
	}

	@BitField(id = 4, optional = true)
	@ReferenceField(stable = false, label = "combatants")
	var onTurn: CombatantReference? = null

	@BitField(id = 5) // TODO right annotation
	@ClassField(root = BattleMove::class)
	var currentMove: BattleMove = BattleMoveThinking

	var selectedMove: BattleMoveSelection = BattleMoveSelectionAttack(target = null)

	@BitField(id = 6)
	var outcome = BattleOutcome.Busy
		private set

	val startTime = System.nanoTime()

	private var updatedTime = 0.seconds
	private var moveDecisionTime = 0.seconds

	@Suppress("unused")
	internal constructor() : this(Battle(), arrayOf(null, null, null, null), CampaignState())

	fun processKeyPress(key: InputKey, soundQueue: SoundQueue) {
		if (onTurn != null && currentMove == BattleMoveThinking) {
			val selectedMove = this.selectedMove
			if (key == InputKey.Cancel) {
				if (selectedMove is BattleMoveSelectionAttack && selectedMove.target != null) {
					this.selectedMove = BattleMoveSelectionAttack(target = null)
				} else if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill != null) {
					this.selectedMove = BattleMoveSelectionSkill(skill = null, target = null)
				} else if (selectedMove is BattleMoveSelectionItem && selectedMove.item != null) {
					this.selectedMove = BattleMoveSelectionItem(item = null, target = null)
				} else {
					this.currentMove = BattleMoveWait
					moveDecisionTime = updatedTime
				}

				soundQueue.insert("click-cancel")
			}

			if (key == InputKey.Interact) {
				if (selectedMove is BattleMoveSelectionWait) {
					this.currentMove = BattleMoveWait
					moveDecisionTime = updatedTime
					soundQueue.insert("click-cancel")
				}
				if (selectedMove is BattleMoveSelectionFlee) {
					outcome = BattleOutcome.RanAway
					moveDecisionTime = updatedTime
				}
			}

			if (key == InputKey.MoveLeft || key == InputKey.MoveRight) {
				if (selectedMove is BattleMoveSelectionAttack && selectedMove.target == null) {
					if (key == InputKey.MoveLeft) this.selectedMove = BattleMoveSelectionSkill(skill = null, target = null)
					else this.selectedMove = BattleMoveSelectionFlee
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionSkill && selectedMove.skill == null) {
					if (key == InputKey.MoveLeft) this.selectedMove = BattleMoveSelectionItem(item = null, target = null)
					else this.selectedMove = BattleMoveSelectionAttack(target = null)
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionItem && selectedMove.item == null) {
					if (key == InputKey.MoveLeft) this.selectedMove = BattleMoveSelectionWait
					else this.selectedMove = BattleMoveSelectionSkill(skill = null, target = null)
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionWait) {
					if (key == InputKey.MoveLeft) this.selectedMove = BattleMoveSelectionFlee
					else this.selectedMove = BattleMoveSelectionItem(item = null, target = null)
					soundQueue.insert("menu-scroll")
				}
				if (selectedMove is BattleMoveSelectionFlee) {
					if (key == InputKey.MoveLeft) this.selectedMove = BattleMoveSelectionAttack(target = null)
					else this.selectedMove = BattleMoveSelectionWait
					soundQueue.insert("menu-scroll")
				}
			}
		}
	}

	internal fun getPlayerStat(stat: CombatStat, index: Int): Int {
		val player = players[index]!!
		val state = playerStates[index]!!
		val base = player.baseStats.find { it.stat == stat }?.adder ?: 0
		val extra = state.statModifiers[stat] ?: 0
		return base + extra
	}

	internal fun getMonsterStat(stat: CombatStat, index: Int): Int {
		val monster = battle.enemies[index]!!.monster
		val state = enemyStates[index]!!
		val base = monster.baseStats[stat] ?: 0
		val extra = state.statModifiers[stat] ?: 0
		return base + extra
	}

	private fun updateOnTurn(soundQueue: SoundQueue) {
		val combatants = allPossibleCombatants.filter { it.isAlive() }
		if (combatants.none { it.isPlayer }) outcome = BattleOutcome.GameOver
		if (combatants.none { !it.isPlayer }) outcome = BattleOutcome.Victory
		if (outcome != BattleOutcome.Busy) return

		val simulator = TurnOrderSimulator(this)
		if (simulator.checkReset()) {
			for (combatant in combatants) combatant.getState().spentTurnsThisRound = 0
		}
		beginTurn(soundQueue, simulator.next()!!)
	}

	private fun beginTurn(soundQueue: SoundQueue, combatant: CombatantReference) {
		combatant.getState().spentTurnsThisRound += 1
		// TODO Allow status effects to skip the turn
		onTurn = combatant

		currentMove = if (combatant.isPlayer) {
			soundQueue.insert("menu-party-scroll")
			selectedMove = BattleMoveSelectionAttack(target = null)
			BattleMoveThinking
		} else {
			moveDecisionTime = updatedTime
			BattleMoveWait
		}
	}

	fun update(soundQueue: SoundQueue, timeStep: Duration) {
		updatedTime += timeStep
		while (onTurn == null && outcome == BattleOutcome.Busy) updateOnTurn(soundQueue)
		if (outcome != BattleOutcome.Busy) return

		if (currentMove == BattleMoveWait && updatedTime > moveDecisionTime + 500.milliseconds) {
			onTurn = null
		}
	}
}
