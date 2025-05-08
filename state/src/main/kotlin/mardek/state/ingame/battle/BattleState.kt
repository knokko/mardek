package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.battle.PartyLayout
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CombatStat
import mardek.input.InputKey
import mardek.state.SoundQueue
import mardek.state.ingame.CampaignState
import mardek.state.ingame.characters.CharacterState
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

	val allPossibleCombatants = (0 until 4).flatMap { listOf(
		CombatantReference(false, it, this), CombatantReference(true, it, this)
	) }

	@BitField(id = 3)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val enemyStates = Array(4) { index ->
		val enemy = battle.enemies[index] ?: return@Array null
		CombatantState(enemy)
	}

	@BitField(id = 4)
	@NestedFieldSetting(path = "", sizeField = IntegerField(expectUniform = true, minValue = 4, maxValue = 4))
	@ReferenceFieldTarget(label = "combatants")
	val playerStates = Array(4) { index ->
		val player = players[index] ?: return@Array null
		CombatantState(player, campaignState.characterStates[player]!!)
	}

	@BitField(id = 5, optional = true)
	@ReferenceField(stable = false, label = "combatants")
	var onTurn: CombatantReference? = null

	@BitField(id = 6) // TODO right annotation
	@ClassField(root = BattleMove::class)
	var currentMove: BattleMove = BattleMoveThinking

	var selectedMove: BattleMoveSelection = BattleMoveSelectionAttack(target = null)

	@BitField(id = 7)
	var outcome = BattleOutcome.Busy
		private set

	val startTime = System.nanoTime()

	private var updatedTime = 0.seconds
	private var moveDecisionTime = 0.seconds

	@Suppress("unused")
	internal constructor() : this(Battle(), arrayOf(null, null, null, null), PartyLayout(), CampaignState())

	internal fun confirmMove(chosenMove: BattleMove, soundQueue: SoundQueue) {
		this.selectedMove = BattleMoveSelectionAttack(target = null)
		this.currentMove = chosenMove
		this.moveDecisionTime = updatedTime
		if (currentMove is BattleMoveWait) soundQueue.insert("click-cancel")
		else soundQueue.insert("click-confirm")
	}

	internal fun runAway() {
		outcome = BattleOutcome.RanAway
		moveDecisionTime = updatedTime
	}

	fun processKeyPress(
		key: InputKey, characterStates: HashMap<PlayableCharacter, CharacterState>, soundQueue: SoundQueue
	) {
		val onTurn = this.onTurn
		if (onTurn != null && currentMove == BattleMoveThinking) {
			if (key == InputKey.Cancel) battleCancel(this, soundQueue)
			if (key == InputKey.Interact) battleClick(this, soundQueue, characterStates)
			if (key == InputKey.MoveLeft || key == InputKey.MoveRight) battleScrollHorizontally(this, key, soundQueue)
			if (key == InputKey.MoveUp || key == InputKey.MoveDown) battleScrollVertically(this, key, soundQueue, characterStates)
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

	fun update(
		characterStates: HashMap<PlayableCharacter, CharacterState>,
		soundQueue: SoundQueue, timeStep: Duration
	) {
		updatedTime += timeStep
		while (onTurn == null && outcome == BattleOutcome.Busy) updateOnTurn(soundQueue)
		if (outcome != BattleOutcome.Busy) return

		if (currentMove == BattleMoveWait && updatedTime > moveDecisionTime + 500.milliseconds) {
			onTurn = null
		}
	}
}
