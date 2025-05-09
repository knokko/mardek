package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import mardek.content.skill.SkillTargetType
import mardek.content.stats.Element
import java.util.*

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

	@BitField(id = 2, optional = true)
	@ReferenceField(stable = true, label = "elements")
	val nextElement: Element?
) : BattleMove() {

	init {
		if (skill.targetType == SkillTargetType.Self || skill.targetType == SkillTargetType.Single) {
			if (target !is BattleSkillTargetSingle) throw IllegalArgumentException(
				"Illegal multi-target $target for single-target skill ${skill.name}"
			)
		}
		if (skill.targetType == SkillTargetType.AllAllies && target !is BattleSkillTargetAllAllies) {
			throw IllegalArgumentException("all-allies skill ${skill.name} targets $target instead of all allies")
		}
		if (skill.targetType == SkillTargetType.AllEnemies && target !is BattleSkillTargetAllEnemies) {
			throw IllegalArgumentException("all-enemies skill ${skill.name} targets $target instead of all enemies")
		}
		if (skill.changeElement && nextElement == null) {
			throw IllegalArgumentException("Elemental shift requires an element")
		}
		if (!skill.changeElement && nextElement != null) {
			throw IllegalArgumentException("Unexpected next element $nextElement for skill $skill")
		}
	}

	@Suppress("unused")
	private constructor() : this(ActiveSkill(), BattleSkillTargetAllEnemies, null)

	override fun equals(other: Any?) = other is BattleMoveSkill &&
			this.skill === other.skill && this.target == other.target

	override fun hashCode() = skill.hashCode() - 13 * target.hashCode()

	override fun toString() = "($skill at $target${if (nextElement != null) " ($nextElement)" else ""})"
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
) : BattleSkillTarget() {

	override fun equals(other: Any?) = other is BattleSkillTargetSingle && this.target == other.target

	override fun hashCode() = 2 + target.hashCode()

	override fun toString() = target.toString()
}

/**
 * Targets all enemies of the caster:
 * - when the caster is a monster, it targets all players
 * - when the caster is a player, it targets all monsters
 */
@BitStruct(backwardCompatible = true)
data object BattleSkillTargetAllEnemies : BattleSkillTarget()

/**
 * Targets all allies of the caster:
 * - when the caster is a monster, it targets all monsters
 * - when the caster is a player, it targets all players
 */
@BitStruct(backwardCompatible = true)
data object BattleSkillTargetAllAllies : BattleSkillTarget()

sealed class BattleMoveSelection {

	open fun targets(state: BattleState) = emptyArray<CombatantReference>()
}

class BattleMoveSelectionAttack(val target: CombatantReference?) : BattleMoveSelection() {

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)

	override fun equals(other: Any?) = other is BattleMoveSelectionAttack && this.target == other.target

	override fun hashCode() = 5 + Objects.hashCode(target)

	override fun toString() = "Attack $target"
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

	override fun equals(other: Any?) = other is BattleMoveSelectionSkill &&
			this.skill === other.skill && this.target == other.target

	override fun hashCode() = 6 + 13 * Objects.hashCode(skill) - 31 * Objects.hashCode(target)

	override fun toString() = "SelectingSkill($skill, $target)"
}

class BattleMoveSelectionItem(val item: Item?, val target: CombatantReference?) : BattleMoveSelection() {

	init {
		if (item == null && target != null) throw IllegalArgumentException("target must be null if item is null")
	}

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)

	override fun equals(other: Any?) = other is BattleMoveSelectionItem &&
			this.item === other.item && this.target == other.target

	override fun hashCode() = 7 + 13 * Objects.hashCode(item) + 31 * Objects.hashCode(target)

	override fun toString() = "SelectingItem($item, $target)"
}

data object BattleMoveSelectionWait : BattleMoveSelection()

data object BattleMoveSelectionFlee : BattleMoveSelection()
