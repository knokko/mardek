package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import java.util.*

sealed class BattleSkillTarget {

	abstract fun getTargets(caster: CombatantState, battle: BattleState): List<CombatantState>

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
	@ReferenceField(stable = false, label = "combatants")
	val target: CombatantState
) : BattleSkillTarget() {

	override fun equals(other: Any?) = other is BattleSkillTargetSingle && this.target === other.target

	override fun hashCode() = 2 + target.hashCode()

	override fun toString() = target.toString()

	override fun getTargets(caster: CombatantState, battle: BattleState) = listOf(target)
}

/**
 * Targets all enemies of the caster:
 * - when the caster is a monster, it targets all players
 * - when the caster is a player, it targets all monsters
 */
@BitStruct(backwardCompatible = true)
data object BattleSkillTargetAllEnemies : BattleSkillTarget() {
	override fun getTargets(
		caster: CombatantState,
		battle: BattleState
	) = if (caster.isOnPlayerSide) battle.livingOpponents() else battle.livingPlayers()
}

/**
 * Targets all allies of the caster:
 * - when the caster is a monster, it targets all monsters
 * - when the caster is a player, it targets all players
 */
@BitStruct(backwardCompatible = true)
data object BattleSkillTargetAllAllies : BattleSkillTarget() {
	override fun getTargets(
		caster: CombatantState,
		battle: BattleState
	) = if (caster.isOnPlayerSide) battle.livingPlayers() else battle.livingOpponents()
}

sealed class BattleMoveSelection {
	open fun targets(state: BattleState) = emptyArray<CombatantState>()
}

class BattleMoveSelectionAttack(val target: CombatantState?) : BattleMoveSelection() {

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)

	override fun equals(other: Any?) = other is BattleMoveSelectionAttack && this.target === other.target

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
		is BattleSkillTargetAllAllies -> state.allPlayers().toTypedArray()
		is BattleSkillTargetAllEnemies -> state.livingOpponents().toTypedArray()
	}

	override fun equals(other: Any?) = other is BattleMoveSelectionSkill &&
			this.skill === other.skill && this.target == other.target

	override fun hashCode() = 6 + 13 * Objects.hashCode(skill) - 31 * Objects.hashCode(target)

	override fun toString() = "SelectingSkill($skill, $target)"
}

class BattleMoveSelectionItem(val item: Item?, val target: CombatantState?) : BattleMoveSelection() {

	init {
		if (item == null && target != null) throw IllegalArgumentException("target must be null if item is null")
	}

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)

	override fun equals(other: Any?) = other is BattleMoveSelectionItem &&
			this.item === other.item && this.target === other.target

	override fun hashCode() = 7 + 13 * Objects.hashCode(item) + 31 * Objects.hashCode(target)

	override fun toString() = "SelectingItem($item, $target)"
}

data object BattleMoveSelectionWait : BattleMoveSelection()

data object BattleMoveSelectionFlee : BattleMoveSelection()
