package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import java.util.*

/**
 * Represents the target of an active skill or attack in combat. This can be either:
 * - a single combatant, or
 * - all players, or
 * - all enemies
 */
sealed class BattleSkillTarget {

	/**
	 * Gets all the target combatants
	 */
	abstract fun getTargets(caster: CombatantState, battle: BattleState): Array<CombatantState>

	companion object {

		@JvmStatic
		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			BattleSkillTargetSingle::class.java,
			BattleSkillTargetAllEnemies::class.java,
			BattleSkillTargetAllAllies::class.java
		)
	}
}

/**
 * A single-combatant [BattleSkillTarget]
 */
@BitStruct(backwardCompatible = true)
class BattleSkillTargetSingle(

	/**
	 * The (only) target combatant
	 */
	@BitField(id = 0)
	@ReferenceField(stable = false, label = "combatants")
	val target: CombatantState
) : BattleSkillTarget() {

	override fun equals(other: Any?) = other is BattleSkillTargetSingle && this.target === other.target

	override fun hashCode() = 2 + target.hashCode()

	override fun toString() = target.toString()

	override fun getTargets(caster: CombatantState, battle: BattleState) = arrayOf(target)
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
	) = if (caster.isOnPlayerSide) battle.livingOpponents().toTypedArray()
	else battle.livingPlayers().toTypedArray()
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
	) = if (caster.isOnPlayerSide) battle.livingPlayers().toTypedArray()
	else battle.livingOpponents().toTypedArray()
}

/**
 * This class captures the move that the player is considering to use (when a playable character is on turn).
 */
sealed class BattleMoveSelection {

	/**
	 * The targets that the player has currently selected (but not yet confirmed).
	 *
	 * To indicate this, the renderer should highlight these characters, and point 'crystal pointers' at them.
	 *
	 * This array can be empty, which means that the player is not yet selecting targets.
	 */
	open fun targets(state: BattleState) = emptyArray<CombatantState>()
}

/**
 * The player is currently considering to use a basic attack.
 */
class BattleMoveSelectionAttack(

	/**
	 * - When this field is `null`, the player is not yet in the target selection mode. The player can enter the
	 * target selection by pressing the interact button (E or X).
	 * - When this field is non-null, the player is currently in the target selection mode, and is considering to
	 * attack this target. The player can use the Interact key to confirm the target, or the arrow keys to pick a
	 * different target.
	 */
	val target: CombatantState?
) : BattleMoveSelection() {

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)

	override fun equals(other: Any?) = other is BattleMoveSelectionAttack && this.target === other.target

	override fun hashCode() = 5 + Objects.hashCode(target)

	override fun toString() = "Attack $target"
}

/**
 * The player is currently considering to cast/use a skill
 */
class BattleMoveSelectionSkill(

	/**
	 * - When this field is `null`, the player is not yet selecting a skill. The player can start selecting a
	 * skill by pressing the Interact key.
	 * - When this field is non-null and [target] is `null`, the player is currently choosing a skill, and this field
	 * is the currently-selected skill. The player can use the Interact key to choose the skill and move on to the
	 * target selection. Alternatively, the player can use the arrow keys to select a different skill.
	 * - When this field is non-null and [target] is also non-null, the player has chosen this skill, and is currently
	 * selecting a target for this skill.
	 */
	val skill: ActiveSkill?,

	/**
	 * - When this field is `null`, the player is not yet selecting a target.
	 * - When this field is non-null, the player must have selected a skill, and is currently choosing a target. This
	 * target is the currently-selected target. The player can confirm this target by pressing the Interact key, or
	 * select a different target by using the arrow keys.
	 */
	val target: BattleSkillTarget?
) : BattleMoveSelection() {

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

/**
 * The player is currently considering to consume an item.
 */
class BattleMoveSelectionItem(
	/**
	 * - When this field is `null`, the player is not yet selecting an item. The player can start selecting an item
	 * by pressing the Interact key.
	 * - When this field is non-null and [target] is `null`, the player is currently choosing an item, and this field
	 * is the currently-selected item. The player can use the Interact key to choose the item and move on to the
	 * target selection. Alternatively, the player can use the arrow keys to select a different item.
	 * - When this field is non-null and [target] is also non-null, the player has chosen this item, and is currently
	 * selecting a target for this item.
	 */
	val item: Item?,

	/**
	 * - When this field is `null`, the player is not yet selecting a target.
	 * - When this field is non-null, the player must have selected an item, and is currently choosing a target. This
	 * target is the currently-selected target. The player can confirm this target by pressing the Interact key, or
	 * select a different target by using the arrow keys.
	 */
	val target: CombatantState?
) : BattleMoveSelection() {

	init {
		if (item == null && target != null) throw IllegalArgumentException("target must be null if item is null")
	}

	override fun targets(state: BattleState) = if (target == null) super.targets(state) else arrayOf(target)

	override fun equals(other: Any?) = other is BattleMoveSelectionItem &&
			this.item === other.item && this.target === other.target

	override fun hashCode() = 7 + 13 * Objects.hashCode(item) + 31 * Objects.hashCode(target)

	override fun toString() = "SelectingItem($item, $target)"
}

/**
 * The player is considering to skip this turn. The player can confirm by pressing the Interact key, or choose a
 * different move by using the arrow keys.
 */
data object BattleMoveSelectionWait : BattleMoveSelection()

/**
 * The player is considering to run away from the battle. The player can confirm by pressing the Interact key,
 * or choose a different move by using the arrow keys.
 */
data object BattleMoveSelectionFlee : BattleMoveSelection()
