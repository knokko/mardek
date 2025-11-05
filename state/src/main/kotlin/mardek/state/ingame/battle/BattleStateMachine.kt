package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.serialize.BitPostInit
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

		/**
		 * When the attacker and/or target are player characters, and they have relevant reaction skills,
		 * this `reactionChallenge` will be non-null. These reaction skills will be applied if and only if this
		 * reaction challenge is passed. Furthermore, if needed, the damage calculation will be postponed until the
		 * outcome of the reaction challenge has been determined.
		 */
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

			/**
			 * Checks whether the reaction challenge is currently *pending*. While the reaction challenge is pending,
			 * the damage calculation must be postponed, since the outcome of the reaction challenge can influence the
			 * damage dealt.
			 *
			 * When there is no reaction challenge (e.g. there are no relevant reactions), this method will always
			 * return false.
			 */
			fun isReactionChallengePending() = this.reactionChallenge != null && this.reactionChallenge.isPending()
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
	) : BattleStateMachine(), Move, BitPostInit {

		/**
		 * The time (`System.nanoTime()`) at which the caster started casting the skill
		 */
		var startTime = System.nanoTime()
			private set

		/**
		 * When the caster and/or target of this skill are player characters, and they have relevant reaction skills,
		 * this `reactionChallenge` will be non-null. These reaction skills will be applied if and only if this
		 * reaction challenge is passed. Furthermore, if needed, the damage calculation will be postponed until the
		 * outcome of the reaction challenge has been determined.
		 */
		@BitField(id = 4, optional = true)
		var reactionChallenge: ReactionChallenge?
			private set

		/**
		 * This field is initially null, which means that the result/effect of the skill has not been calculated yet.
		 *
		 * When the move result/damage has been calculated, it will be stored in this array. This array will contain
		 * 1 entry for every target of the skill. Initially, all entries in this array will be non-null. For each
		 * target, the damage will be applied at time `particle start time + damage delay` (note that each target has
		 * a different `particle start time`). Once the damage has been applied to a target, the corresponding entry is
		 * set to null, ensuring that each target only takes damage once.
		 *
		 * Any potential effects (e.g. life steal) will be applied to the *caster* at the same time when this array is
		 * created,
		 */
		@BitField(id = 5)
		@NestedFieldSetting(
			path = "", optional = true,
			sizeField = IntegerField(expectUniform = true, minValue = 1, maxValue = 4)
		)
		@NestedFieldSetting(path = "c", optional = true)
		var calculatedDamage: Array<MoveResult.Entry?>? = null

		/**
		 * The renderer will set this field to true once the casting animation has progressed far enough to start
		 * spawning the particles at the position of the (first) target.
		 */
		var canSpawnTargetParticles = false

		/**
		 * The renderer will set this field to true once the caster has finished its casting animation. The next turn
		 * will be blocked until (among others) the casting animation is finished.
		 */
		var hasFinishedCastingAnimation = false

		/**
		 * While casing magic skills, a small particle effect (depending on the element of the skill) is spawned
		 * continuously at the main hand of the caster. This field tracks when it was last spawned, to keep the rate
		 * at a stable 30 particles per second.
		 */
		var lastCastParticleSpawnTime = 0L

		/**
		 * All magic skills have a particle effect that is displayed at the position of the target(s).
		 * This field records the time at which that particle was spawned at the *first* target.
		 * - the particle for the second target will be spawned 250ms later
		 * - the particle for the third target will be spawned 500ms later
		 * - the particle for the fourth target will be spawned 750ms later
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

		override fun postInit(context: BitPostInit.Context) {
			if (calculatedDamage != null) reactionChallenge = null
		}

		override fun equals(other: Any?) = other is CastSkill && caster === other.caster &&
				targets.contentEquals(other.targets) && skill === other.skill && nextElement === other.nextElement

		override fun hashCode() = caster.hashCode() + 13 * targets.hashCode() - 31 * skill.hashCode() +
				127 * Objects.hashCode(nextElement)

		override fun refreshStartTime() {
			startTime = System.nanoTime()
		}

		/**
		 * Checks whether the reaction challenge is currently *pending*. While the reaction challenge is pending, the
		 * damage calculation must be postponed, since the outcome of the reaction challenge can influence the damage
		 * dealt.
		 *
		 * When there is no reaction challenge (e.g. there are no relevant reactions), this method will always return
		 * false.
		 */
		fun isReactionChallengePending() = reactionChallenge != null && reactionChallenge!!.isPending()

		/**
		 * Checks whether all skill damage/effects have been applied to all targets. The battle must not transition to
		 * the next state until this has happened.
		 */
		fun hasAppliedAllDamage() = calculatedDamage != null && calculatedDamage!!.all { it == null }
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
