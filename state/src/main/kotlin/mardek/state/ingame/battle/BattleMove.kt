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
data object BattleMoveWait : BattleMove()

@BitStruct(backwardCompatible = true)
class BattleMoveItem(
	@BitField(id = 0)
	@ReferenceField(stable = true, label = "items")
	val item: Item,

	@BitField(id = 1)
	val target: CombatantReference,
) : BattleMove() {

	@Suppress("unused")
	private constructor() : this(Item(), CombatantReference())
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
	val target: CombatantReference
) {

	@Suppress("unused")
	private constructor() : this(CombatantReference())
}

sealed class BattleSkillTarget

class BattleSkillTargetSingle(
	val target: CombatantReference
) : BattleSkillTarget()

data object BattleSkillTargetAllEnemies : BattleSkillTarget()

data object BattleSkillTargetAllAllies : BattleSkillTarget()

sealed class BattleMoveSelection

class BattleMoveSelectionAttack(val target: BattleSkillTarget?) : BattleMoveSelection()

class BattleMoveSelectionSkill(val skill: ActiveSkill?, val target: BattleSkillTarget?) : BattleMoveSelection()

class BattleMoveSelectionItem(val item: Item?, val target: CombatantReference?) : BattleMoveSelection()

data object BattleMoveSelectionWait : BattleMoveSelection()

data object BattleMoveSelectionFlee : BattleMoveSelection()
