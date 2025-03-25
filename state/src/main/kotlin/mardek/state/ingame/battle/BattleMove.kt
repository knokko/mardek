package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill

@BitStruct(backwardCompatible = true)
sealed class BattleMove {
	val decisionTime = System.nanoTime()

	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			BattleMoveThinking::class.java,
			BattleMoveWait::class.java,
			BattleMoveItem::class.java,
			BattleMoveSkill::class.java,
			BattleMoveBasicAttack::class.java
		)
	}
}

@BitStruct(backwardCompatible = true)
data object BattleMoveThinking : BattleMove()

@BitStruct(backwardCompatible = true)
class BattleMoveWait : BattleMove()

@BitStruct(backwardCompatible = true)
class BattleMoveItem(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "items")
	val item: Item,

	@BitField(id = 1)
	@ReferenceField(stable = false, label = "combatants")
	val target: CombatantState,
) : BattleMove() {

	@Suppress("unused")
	private constructor() : this(Item(), CombatantState())
}

@BitStruct(backwardCompatible = true)
class BattleMoveSkill(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "skills")
	val skill: ActiveSkill,
) {

	@Suppress("unused")
	private constructor() : this(ActiveSkill())
}

@BitStruct(backwardCompatible = true)
data class BattleMoveBasicAttack(
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "combatants")
	val target: CombatantState
) {

	@Suppress("unused")
	private constructor() : this(CombatantState())
}

sealed class BattleSkillTarget

class BattleSkillTargetSingle(
	val target: CombatantState
) : BattleSkillTarget()

data object BattleSkillTargetAllEnemies : BattleSkillTarget()

data object BattleSkillTargetAllAllies : BattleSkillTarget()
