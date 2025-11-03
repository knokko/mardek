package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.inventory.Item
import mardek.content.particle.ParticleEffect
import mardek.content.skill.ActiveSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.skill.SkillTargetType
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import java.util.Objects

@BitStruct(backwardCompatible = true)
sealed class BattleStateMachine {

	companion object {

		@JvmStatic
		@Suppress("unused")
		val BITSER_HIERARCHY = arrayOf(
			NextTurn::class.java,
			NextTurnEffects::class.java,
			SelectMove::class.java,
			Wait::class.java,
			MeleeAttack.MoveTo::class.java,
			MeleeAttack.Strike::class.java,
			MeleeAttack.JumpBack::class.java,
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

	@BitStruct(backwardCompatible = true)
	class NextTurnEffects(
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val combatant: CombatantState,

		@BitField(id = 1, optional = true)
		val forceMove: ForceMove?,

	) : BattleStateMachine() {

		@Suppress("unused")
		private constructor() : this(MonsterCombatantState(), null)

		@BitField(id = 2)
		@ReferenceField(stable = true, label = "status effects")
		val removedEffects = HashSet<StatusEffect>()

		@BitField(id = 3)
		val takeDamage = ArrayList<TakeDamage>()

		var applyNextDamageAt = System.nanoTime()

		@BitStruct(backwardCompatible = true)
		class ForceMove(
			@BitField(id = 0)
			@ClassField(root = BattleStateMachine::class)
			val move: Move,

			@BitField(id = 1)
			@ReferenceField(stable = true, label = "status effects")
			val effect: StatusEffect,

			val blinkColor: Int,

			val particleEffect: ParticleEffect?,
		) {
			@Suppress("unused")
			private constructor() : this(Wait(), StatusEffect(), 0, null)
		}

		@BitStruct(backwardCompatible = true)
		class TakeDamage(
			@BitField(id = 0)
			@IntegerField(expectUniform = false)
			val amount: Int,

			@BitField(id = 1)
			@ReferenceField(stable = true, label = "status effects")
			val effect: StatusEffect,
		) {
			@Suppress("unused")
			private constructor() : this(0, StatusEffect())
		}

		companion object {
			const val DAMAGE_DELAY = 1_000_000_000L
		}
	}

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

		@Suppress("unused")
		private constructor() : this(PlayerCombatantState())

		override fun toString() = "$onTurn considers $selectedMove"
	}

	/**
	 * A player (or monster) has decided to wait (skip) its turn, so the state should switch to the next combatant
	 * after a short while
	 */
	@BitStruct(backwardCompatible = true)
	class Wait : BattleStateMachine(), Move {
		var startTime = System.nanoTime()

		override fun refreshStartTime() {
			startTime = System.nanoTime()
		}
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
		var startTime = System.nanoTime()

		constructor() : this(
			MonsterCombatantState(), MonsterCombatantState(),
			null, null
		)

		override fun refreshStartTime() {
			startTime = System.nanoTime()
		}

		companion object {
			fun determineReactionChallenge(
				attacker: CombatantState, target: CombatantState,
				skill: ActiveSkill?, context: BattleUpdateContext
			): ReactionChallenge? {
				var primaryType: ReactionSkillType? = null
				val isHealing = skill != null && skill.isPositive() && !target.revertsHealing()

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
			skill: ActiveSkill?, reactionChallenge: ReactionChallenge?, //context: BattleUpdateContext,
		) : MeleeAttack(attacker, target, skill, reactionChallenge) {
			var finished = false

			constructor(
				attacker: CombatantState, target: CombatantState,
				skill: ActiveSkill?, context: BattleUpdateContext,
			) : this(attacker, target, skill,
				determineReactionChallenge(attacker, target, skill, context)
			)

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), MonsterCombatantState(),
				null, null,
			)
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

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), MonsterCombatantState(),
				null, null,
			)
		}

		@BitStruct(backwardCompatible = true)
		class JumpBack(
			attacker: CombatantState, target: CombatantState,
			skill: ActiveSkill?, reactionChallenge: ReactionChallenge?,
		) : MeleeAttack(attacker, target, skill, reactionChallenge) {
			var finished = false

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), MonsterCombatantState(),
				null, null,
			)
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
		val targets: Array<CombatantState>,

		@BitField(id = 2)
		@ReferenceField(stable = true, label = "skills")
		val skill: ActiveSkill,

		@BitField(id = 3, optional = true)
		@ReferenceField(stable = true, label = "elements")
		val nextElement: Element?,

		context: BattleUpdateContext,
	) : BattleStateMachine(), Move {
		var startTime = System.nanoTime()

		@BitField(id = 4, optional = true)
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
				if (target.hasReactions(context, ReactionSkillType.RangedDefense) && (!isHealing || target.revertsHealing())) {
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
			MonsterCombatantState(), emptyArray<CombatantState>(),
			ActiveSkill(), null, BattleUpdateContext()
		)

		override fun equals(other: Any?) = other is CastSkill && caster === other.caster &&
				targets.contentEquals(other.targets) && skill === other.skill && nextElement === other.nextElement

		override fun hashCode() = caster.hashCode() + 13 * targets.hashCode() - 31 * skill.hashCode() +
				127 * Objects.hashCode(nextElement)

		override fun refreshStartTime() {
			startTime = System.nanoTime()
		}
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
	) : BattleStateMachine(), Move {
		var startTime = System.nanoTime()

		var canDrinkItem = false

		@Suppress("unused")
		private constructor() : this(MonsterCombatantState(), MonsterCombatantState(), Item())

		override fun refreshStartTime() {
			startTime = System.nanoTime()
		}
	}

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

		fun shouldGoToGameOverMenu() = (System.nanoTime() - startTime) >= FADE_DURATION

		companion object {
			const val FADE_DURATION = 5000_000_000L
		}
	}

	sealed interface Move {
		fun refreshStartTime()
	}
}
