package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.*
import mardek.content.characters.PlayableCharacter
import mardek.content.stats.CombatStat
import mardek.input.InputKey
import mardek.state.ingame.CampaignState
import kotlin.math.max

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
	var onTurn: CombatantState? = null

	@BitField(id = 5) // TODO right annotation
	@ClassField(root = BattleMove::class)
	var currentMove: BattleMove = BattleMoveThinking

	val startTime = System.nanoTime()

	@Suppress("unused")
	private constructor() : this(Battle(), emptyArray(), CampaignState())

	fun processKeyPress(key: InputKey) {
	}

	fun isFinished(): Boolean {
		return false
	}

	fun getPlayerStat(stat: CombatStat, index: Int): Int? {
		val player = players[index] ?: return null
		val state = playerStates[index]!!
		val base = player.baseStats.find { it.stat == stat }?.adder ?: 0
		val extra = state.statModifiers[stat] ?: 0
		return base + extra
	}

	fun getMonsterStat(stat: CombatStat, index: Int): Int? {
		val state = enemyStates[index] ?: return null
		TODO()
	}

	private fun updateOnTurn() {
		val bestPlayerAgility = (0 until 4).mapNotNull { getPlayerStat(CombatStat.Agility, it) }.max()
		val bestMonsterAgility = (0 until 4).mapNotNull { getMonsterStat(CombatStat.Agility, it) }.max()
		val bestAgility = max(bestPlayerAgility, bestMonsterAgility)
	}

	fun update() {
		if (onTurn == null) updateOnTurn()
		// TODO
	}
}
