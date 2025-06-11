package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.Item
import mardek.content.skill.ActiveSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.skill.SkillTargetType
import mardek.content.stats.Element
import java.util.Objects

@BitStruct(backwardCompatible = true)
sealed class BattleStateMachine {

	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			NextTurn::class.java,
			SelectMove::class.java,
			Wait::class.java,
			MeleeAttack::class.java,
			CastSkill::class.java,
			UseItem::class.java,
			RanAway::class.java,
			Victory::class.java,
			GameOver::class.java,
		)
	}

	/**
	 * The battle should move on to the next combatant that is on turn
	 */
	@BitStruct(backwardCompatible = true)
	class NextTurn(val startAt: Long) : BattleStateMachine()

	/**
	 * A player is on turn, and this player is currently choosing its next move
	 */
	@BitStruct(backwardCompatible = true)
	class SelectMove(
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val onTurn: PlayerCombatantState
	) : BattleStateMachine() {
		var selectedMove: BattleMoveSelection = BattleMoveSelectionAttack(null)
	}

	/**
	 * A player (or monster) has decided to wait (skip) its turn, so the state should switch to the next combatant
	 * after a short while
	 */
	@BitStruct(backwardCompatible = true)
	class Wait : BattleStateMachine(), Move {
		val startTime = System.nanoTime()
	}

	/**
	 * A combatant is doing either a basic attack, or a melee skill
	 */
	@BitStruct(backwardCompatible = true)
	sealed class MeleeAttack(
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val attacker: CombatantState,

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "combatants")
		val target: CombatantState,

		/**
		 * The melee skill that `attacker` is using, or `null` if `attacker` is doing a basic attack
		 */
		@BitField(id = 2, optional = true)
		@ReferenceField(stable = true, label = "skills")
		val skill: ActiveSkill?,

		@BitField(id = 3, optional = true)
		val reactionChallenge: ReactionChallenge?,
	) : BattleStateMachine(), Move {
		val startTime = System.nanoTime()

		companion object {
			fun determineReactionChallenge(
				attacker: CombatantState, target: CombatantState,
				skill: ActiveSkill?, context: BattleUpdateContext
			): ReactionChallenge? {
				var primaryType: ReactionSkillType? = null
				val isHealing = skill != null && skill.isPositive() && !target.getCreatureType().revertsHealing

				if (target.hasReactions(context, ReactionSkillType.MeleeDefense) && !isHealing) {
					primaryType = ReactionSkillType.MeleeDefense
				}
				if (attacker.hasReactions(context, ReactionSkillType.MeleeAttack)) {
					primaryType = ReactionSkillType.MeleeAttack
				}

				return if (primaryType != null) ReactionChallenge(primaryType) else null
			}
		}

		@BitStruct(backwardCompatible = true)
		class MoveTo(
			attacker: CombatantState, target: CombatantState,
			skill: ActiveSkill?, context: BattleUpdateContext,
		) : MeleeAttack(
			attacker, target, skill,
			determineReactionChallenge(attacker, target, skill, context)
		) {
			var finished = false
		}

		@BitStruct(backwardCompatible = true)
		class Strike(
			attacker: CombatantState, target: CombatantState,
			skill: ActiveSkill?, reactionChallenge: ReactionChallenge?,
		) : MeleeAttack(attacker, target, skill, reactionChallenge) {
			var canDealDamage = false

			@BitField(id = 0)
			var hasDealtDamage = false

			var finished = false
		}

		@BitStruct(backwardCompatible = true)
		class JumpBack(
			attacker: CombatantState, target: CombatantState,
			skill: ActiveSkill?, reactionChallenge: ReactionChallenge?,
		) : MeleeAttack(attacker, target, skill, reactionChallenge) {
			var finished = false
		}
	}

	/**
	 * A combatant is casting a ranged skill
	 */
	@BitStruct(backwardCompatible = true)
	class CastSkill(
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val caster: CombatantState,

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "combatants")
		val targets: List<CombatantState>,

		@BitField(id = 2)
		@ReferenceField(stable = true, label = "skills")
		val skill: ActiveSkill,

		@BitField(id = 3, optional = true)
		@ReferenceField(stable = true, label = "elements")
		val nextElement: Element?,

		context: BattleUpdateContext,
	) : BattleStateMachine(), Move {
		val startTime = System.nanoTime()

		@BitField(id = 4)
		val reactionChallenge: ReactionChallenge?

		var canDealDamage = false

		/**
		 * While casing magic skills, a small particle effect (depending on the element of the skill) is spawned
		 * continuously at the main hand of the caster. This field tracks when it was last spawned, to keep the rate
		 * at a stable 30 particles per second.
		 */
		var lastCastParticleSpawnTime = 0L

		/**
		 * All magic skills have a particle effect that is displayed at the position of the target(s).
		 * This field records the time at which that particle was spawned.
		 */
		var targetParticlesSpawnTime = 0L

		init {
			if (skill.targetType == SkillTargetType.Self || skill.targetType == SkillTargetType.Single) {
				if (targets.size > 1) throw IllegalArgumentException(
					"Illegal multi-target ${targets }for single-target skill ${skill.name}"
				)
			}
			if (skill.changeElement && nextElement == null) {
				throw IllegalArgumentException("Elemental shift requires an element")
			}
			if (!skill.changeElement && nextElement != null) {
				throw IllegalArgumentException("Unexpected next element $nextElement for skill $skill")
			}

			var primaryType: ReactionSkillType? = null
			val isHealing = skill.isPositive()
			for (target in targets) {
				if (target.hasReactions(context, ReactionSkillType.RangedDefense) && (!isHealing || target.getCreatureType().revertsHealing)) {
					primaryType = ReactionSkillType.RangedDefense
				}
			}

			if (caster.hasReactions(context, ReactionSkillType.RangedAttack)) {
				primaryType = ReactionSkillType.RangedAttack
			}
			reactionChallenge = if (primaryType != null) ReactionChallenge(primaryType) else null
		}

		@Suppress("unused")
		private constructor() : this(
			MonsterCombatantState(), emptyList<CombatantState>(),
			ActiveSkill(), null, BattleUpdateContext()
		)

		override fun equals(other: Any?) = other is CastSkill && caster === other.caster && targets == other.targets &&
				skill === other.skill && nextElement === other.nextElement

		override fun hashCode() = caster.hashCode() + 13 * targets.hashCode() - 31 * skill.hashCode() +
				127 * Objects.hashCode(nextElement)
	}

	/**
	 * Someone is throwing a consumable item
	 */
	@BitStruct(backwardCompatible = true)
	class UseItem(
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val thrower: CombatantState,

		@BitField(id = 1)
		@ReferenceField(stable = false, label = "combatants")
		val target: CombatantState,

		@BitField(id = 2)
		@ReferenceField(stable = true, label = "items")
		val item: Item
	) : BattleStateMachine(), Move

	/**
	 * The player ran away from the battle, so the game state should go back to the area state
	 */
	@BitStruct(backwardCompatible = true)
	class RanAway : BattleStateMachine()

	/**
	 * All enemies have been slain, so the victory 'dance' should be shown. After a short while, the game should go
	 * to the battle loot menu.
	 */
	@BitStruct(backwardCompatible = true)
	class Victory : BattleStateMachine() {
		val startTime = System.nanoTime()

		fun shouldGoToLootMenu() = (System.nanoTime() - startTime) >= 2000_000_000L
	}

	/**
	 * All players have fainted, so the battle screen should slowly fade black, after which the game should go to the
	 * game-over screen.
	 */
	@BitStruct(backwardCompatible = true)
	class GameOver : BattleStateMachine() {
		val startTime = System.nanoTime()

		fun shouldGoToGameOverMenu() = (System.nanoTime() - startTime) >= 2000_000_000L
	}

	sealed interface Move
}
