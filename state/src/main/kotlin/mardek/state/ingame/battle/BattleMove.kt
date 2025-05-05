package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
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

	@BitField(id = 1)
	@ClassField(root = BattleSkillTarget::class)
	val target: BattleSkillTarget,
) : BattleMove() {

	@Suppress("unused")
	private constructor() : this(ActiveSkill(), BattleSkillTargetAllEnemies)
}

@BitStruct(backwardCompatible = true)
data class BattleMoveBasicAttack(
	@BitField(id = 0)
	val target: CombatantReference
) : BattleMove() {

	@Suppress("unused")
	private constructor() : this(CombatantReference())
}

sealed class BattleSkillTarget {
	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			BattleSkillTargetSingle::class.java,
			BattleSkillTargetAllEnemies::class.java,
			BattleSkillTargetAllAllies::class.java
		)
	}
}

@BitStruct(backwardCompatible = true)
class BattleSkillTargetSingle(

	@BitField(id = 0)
	val target: CombatantReference
) : BattleSkillTarget()

@BitStruct(backwardCompatible = true)
data object BattleSkillTargetAllEnemies : BattleSkillTarget()

@BitStruct(backwardCompatible = true)
data object BattleSkillTargetAllAllies : BattleSkillTarget()

sealed class BattleMoveSelection {

	open fun targets(state: BattleState) = emptyArray<CombatantReference>()
}

class BattleMoveSelectionAttack(val target: CombatantReference?) : BattleMoveSelection() {

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)
}

class BattleMoveSelectionSkill(val skill: ActiveSkill?, val target: BattleSkillTarget?) : BattleMoveSelection() {

	init {
		if (skill == null && target != null) throw IllegalArgumentException("target must be null if skill is null")
	}

	override fun targets(state: BattleState) = when (target) {
		null -> super.targets(state)
		is BattleSkillTargetSingle -> arrayOf(target.target)
		is BattleSkillTargetAllAllies -> state.playerStates.withIndex().filter {
			it.value != null
		}.map { CombatantReference(true, it.index, state) }.toTypedArray()
		is BattleSkillTargetAllEnemies -> state.enemyStates.withIndex().filter {
			it.value != null
		}.map { CombatantReference(false, it.index, state) }.toTypedArray()
	}
}

class BattleMoveSelectionItem(val item: Item?, val target: CombatantReference?) : BattleMoveSelection() {

	init {
		if (item == null && target != null) throw IllegalArgumentException("target must be null if item is null")
	}

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)
}

data object BattleMoveSelectionWait : BattleMoveSelection()

data object BattleMoveSelectionFlee : BattleMoveSelection()
