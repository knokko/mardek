package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.CombatStat

@BitStruct(backwardCompatible = true)
class CombatantReference(

	@BitField(id = 0)
	val isPlayer: Boolean,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 4)
	val index: Int,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "battle state")
	private val battleState: BattleState
) {

	internal constructor() : this(false, 0, BattleState())

	// TODO Properly distinguish missing combatants from dead combatants
	fun isAlive() = if (isPlayer) battleState.playerStates[index] != null
	else battleState.enemyStates[index] != null

	fun getStat(stat: CombatStat) = if (isPlayer) battleState.getPlayerStat(stat, index)
	else battleState.getMonsterStat(stat, index)

	fun getState() = if (isPlayer) battleState.playerStates[index]!!
	else battleState.enemyStates[index]!!
}
