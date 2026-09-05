package mardek.state.ingame.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ClassField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.BitPostInit
import mardek.content.inventory.Item
import mardek.content.particle.ParticleEffect
import mardek.content.skill.ActiveSkill
import mardek.content.skill.ReactionSkillType
import mardek.content.skill.SkillTargetType
import mardek.content.stats.Element
import mardek.content.stats.StatusEffect
import mardek.content.util.Time
import mardek.state.ingame.battle.combatant.CombatantState
import mardek.state.ingame.battle.combatant.MonsterCombatantState
import mardek.state.ingame.battle.combatant.PlayerCombatantState
import java.io.Serializable
import java.util.Objects
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The state machine that is used by [BattleState] to track which combatant is currently on turn, and what that
 * combatant is doing.
 */
@BitStruct(backwardCompatible = true)
sealed class BattleStateMachine : Serializable {

	companion object {

		@JvmStatic
		@Suppress("unused")
		private val BITSER_HIERARCHY = arrayOf(
			NextTurn::class.java,
			NextTurnEffects::class.java,
			SelectMove::class.java,
			Wait::class.java,
			MeleeAttack.MoveTo::class.java,
			MeleeAttack.Strike::class.java,
			MeleeAttack.JumpBack::class.java,
			BreathAttack.MoveTo::class.java,
			BreathAttack.Attack::class.java,
			BreathAttack.JumpBack::class.java,
			CastSkill::class.java,
			UseItem::class.java,
			RanAway::class.java,
			Victory::class.java,
			GameOver::class.java,
		)
	}

	/**
	 * The battle should move on to the next combatant that is on turn,
	 * once `campaignState.time >= lastFinishTime + delay`.
	 */
	@BitStruct(backwardCompatible = true)
	class NextTurn(

		/**
		 * The time at which the previous move/turn ended (or the start of the battle when this is the first turn).
		 */
		@BitField(id = 0)
		val lastFinishTime: Time,

		/**
		 * The delay until the next turn starts, from [lastFinishTime]
		 */
		@BitField(id = 1)
		@IntegerField(expectUniform = false, minValue = 0)
		val delay: Duration,
	) : BattleStateMachine() {

		@Suppress("unused")
		private constructor() : this(Time.ZERO, Duration.ZERO)
	}

	/**
	 * The turn of [combatant] should start soon, but we should first process and render some events, for instance:
	 * - Poison/Bleed damage (if [combatant] is poisoned or bleeding)
	 * - Displaying the status effects that [combatant] is losing (e.g. Sleep)
	 *
	 * Finally, when [forceMove] is non-null, the combatant will be forced to use [ForceMove.move]
	 * (typically [BattleStateMachine.Wait] for e.g. Sleep and Paralysis).
	 */
	@BitStruct(backwardCompatible = true)
	class NextTurnEffects(

		/**
		 * The combatant that will be 'on turn' soon
		 */
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val combatant: CombatantState,

		/**
		 * - When this field is `null`, [combatant] can choose its next move normally.
		 * - When this field is non-null, [combatant] will be forced to use [ForceMove.move]
		 */
		@BitField(id = 1, optional = true)
		val forceMove: ForceMove?,

		/**
		 * The current value of [mardek.state.ingame.CampaignState.time]
		 */
		currentCampaignTime: Time,
	) : BattleStateMachine() {

		@Suppress("unused")
		private constructor() : this(MonsterCombatantState(), null, Time.ZERO)

		/**
		 * The status effects that will be removed at the start of the turn
		 */
		@BitField(id = 2)
		@ReferenceField(stable = true, label = "status effects")
		val removedEffects = HashSet<StatusEffect>()

		/**
		 * The damage that the combatant will take before its turn starts.
		 * When this is non-empty, the turn will be delayed until this list is empty.
		 */
		@BitField(id = 3)
		val takeDamage = ArrayList<TakeDamage>()

		/**
		 * When [takeDamage] is non-empty, this is the time at which the combatant should
		 * lose/gain health due to the first status effect in [takeDamage].
		 *
		 * When `takeDamage.size > 1`, this variable will be increased by a short delay after each element of
		 * [takeDamage] is applied. This should ensure that each damaging/healing status effect is activated slightly
		 * later than the previous status effect.
		 */
		@BitField(id = 4)
		var applyNextDamageAt = currentCampaignTime

		/**
		 * The type/class of [BattleStateMachine.NextTurnEffects.forceMove]: it defines which move is being forced,
		 * with some associated information that is useful for rendering.
		 */
		@BitStruct(backwardCompatible = true)
		class ForceMove(

			/**
			 * The move that the combatant is forced to take. For Sleep and Paralysis, this will always be
			 * [BattleStateMachine.Wait]. For Confusion, there are many more possibilities, especially for
			 * monsters.
			 */
			@BitField(id = 0)
			@ClassField(root = BattleStateMachine::class)
			val move: Move,

			/**
			 * The status effect that forced the combatant to use [move]
			 */
			@BitField(id = 1)
			@ReferenceField(stable = true, label = "status effects")
			val effect: StatusEffect,

			/**
			 * When this field is non-zero, the combatant should blink in this color. This is used by the
			 * Paralysis status effect to cause the 'yellow blink' when a combatant skips a turn due to the paralysis.
			 */
			@BitField(id = 2)
			@IntegerField(expectUniform = true)
			val blinkColor: Int,

			/**
			 * The particle effect that should be played. This is used by the Sleep status effect to display the 'Z's
			 * when the combatant sleeps during a turn.
			 */
			@BitField(id = 3, optional = true)
			@ReferenceField(stable = true, label = "particles")
			val particleEffect: ParticleEffect?,
		) {
			@Suppress("unused")
			private constructor() : this(
				Wait(Time.ZERO),
				StatusEffect(), 0, null,
			)
		}

		/**
		 * This type/class is used for [BattleStateMachine.NextTurnEffects.takeDamage]: it is just a tuple
		 * (damageAmount, statusEffect).
		 */
		@BitStruct(backwardCompatible = true)
		class TakeDamage(

			/**
			 * The amount of damage that the combatant takes due to the status effect. When this is negative, the
			 * combatant will heal instead (used by Regeneration).
			 */
			@BitField(id = 0)
			@IntegerField(expectUniform = false)
			val amount: Int,

			/**
			 * The status effect that causes the damage (or healing)
			 */
			@BitField(id = 1)
			@ReferenceField(stable = true, label = "status effects")
			val effect: StatusEffect,
		) {
			@Suppress("unused")
			private constructor() : this(0, StatusEffect())
		}

		companion object {

			/**
			 * The delay, between applying two elements of [takeDamage].
			 * This is only relevant when `takeDamage.size > 1`.
			 */
			val DAMAGE_DELAY = 1.seconds
		}
	}

	/**
	 * A player is on turn, and this player is currently choosing its next move
	 */
	@BitStruct(backwardCompatible = true)
	class SelectMove(

		/**
		 * The playable combatant that is on turn
		 */
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val onTurn: PlayerCombatantState
	) : BattleStateMachine() {

		/**
		 * The currently-selected (but not yet confirmed) move
		 */
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
	class Wait(

		/**
		 * The current value of [mardek.state.ingame.CampaignState.time]
		 */
		currentCampaignTime: Time,
	) : BattleStateMachine(), Move {

		/**
		 * The value of [mardek.state.ingame.CampaignState.time] when the combatant decided to skip its turn.
		 */
		@BitField(id = 0)
		val startTime = currentCampaignTime

		@Suppress("unused")
		private constructor() : this(Time.ZERO)
	}

	/**
	 * A combatant is doing either a basic attack, or a melee skill.
	 *
	 * Note that this is a sealed class. The subclasses are [MoveTo], [Strike], and [JumpBack].
	 */
	@BitStruct(backwardCompatible = true)
	sealed class MeleeAttack(

		/**
		 * The attacking combatant
		 */
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val attacker: CombatantState,

		/**
		 * The combatant that is being attacked
		 */
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

		/**
		 * The current value of [mardek.state.ingame.CampaignState.time]
		 */
		currentCampaignTime: Time,
	) : BattleStateMachine(), Move {

		/**
		 * The value of [mardek.state.ingame.CampaignState.time] when the battle transitioned to this state.
		 */
		@BitField(id = 4)
		val startTime = currentCampaignTime

		constructor() : this(
			MonsterCombatantState(), MonsterCombatantState(),
			null, null, Time.ZERO,
		)

		companion object {

			/**
			 * Determines the reaction challenge that the player should get, if any
			 */
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

				return if (primaryType != null) ReactionChallenge(primaryType, context.campaignTime) else null
			}
		}

		/**
		 * The first phase of [MeleeAttack]: the attacker runs towards the target
		 */
		@BitStruct(backwardCompatible = true)
		class MoveTo(
			attacker: CombatantState, target: CombatantState,
			skill: ActiveSkill?, reactionChallenge: ReactionChallenge?,
			currentCampaignTime: Time,
		) : MeleeAttack(attacker, target, skill, reactionChallenge, currentCampaignTime) {

			/**
			 * Whether the attacker is at least halfway to the target.
			 * This is useful for the rendering order/depth.
			 */
			var halfWay = false

			/**
			 * Whether the attacker has reached the target. The renderer should set this to `true` when the move-to
			 * animation is finished, after which the state should transition it to [Strike].
			 */
			var finished = false

			constructor(
				attacker: CombatantState, target: CombatantState,
				skill: ActiveSkill?, context: BattleUpdateContext,
			) : this(
				attacker, target, skill,
				determineReactionChallenge(attacker, target, skill, context),
				context.campaignTime,
			)

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), MonsterCombatantState(),
				null, null, Time.ZERO,
			)
		}

		/**
		 * The second phase of [MeleeAttack]: this is the phase where the attacker strikes/hits the target, and deals
		 * the damage.
		 */
		@BitStruct(backwardCompatible = true)
		class Strike(
			attacker: CombatantState, target: CombatantState,
			skill: ActiveSkill?, reactionChallenge: ReactionChallenge?,
			currentCampaignTime: Time,
		) : MeleeAttack(attacker, target, skill, reactionChallenge, currentCampaignTime) {

			/**
			 * The renderer should set this to `true` when the attacker is ~halfway the strike animation.
			 * When this is `true`, the state should deal the damage, and set [hasDealtDamage] to `true`.
			 */
			var canDealDamage = false

			/**
			 * The state should set this to `true` after it has 'processed' the damage calculation, and deducted the
			 * health from the target.
			 */
			@BitField(id = 0)
			var hasDealtDamage = false

			/**
			 * The renderer should set this to `true` after it has finished rendering the strike animation of the
			 * attacker. Once this is `true`, the state can be changed to [JumpBack].
			 */
			var finished = false

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), MonsterCombatantState(),
				null, null, Time.ZERO,
			)

			/**
			 * Checks whether the reaction challenge is currently *pending*. While the reaction challenge is pending,
			 * the damage calculation must be postponed, since the outcome of the reaction challenge can influence the
			 * damage dealt.
			 *
			 * When there is no reaction challenge (e.g. there are no relevant reactions), this method will always
			 * return false.
			 */
			fun isReactionChallengePending(currentTime: Time) = this.reactionChallenge != null &&
					this.reactionChallenge.isPending(currentTime)
		}

		/**
		 * The attacker has finished its attack (and has dealt damage). During this state, the attacker will walk/jump
		 * back to its original position.
		 */
		@BitStruct(backwardCompatible = true)
		class JumpBack(
			attacker: CombatantState, target: CombatantState,
			skill: ActiveSkill?, reactionChallenge: ReactionChallenge?,
			currentCampaignTime: Time,
		) : MeleeAttack(attacker, target, skill, reactionChallenge, currentCampaignTime) {

			/**
			 * Whether the attacker is at least halfway on its way back to its original position.
			 * This is useful for the rendering order/depth.
			 */
			var halfWay = false

			/**
			 * Whether the attacker has reached its original position. The renderer should set this to `true` when
			 * the jump-back animation is finished, after which the state should transition it to
			 * [BattleStateMachine.NextTurn].
			 */
			var finished = false

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), MonsterCombatantState(),
				null, null, Time.ZERO,
			)
		}
	}

	/**
	 * A combatant is doing a (fire)breath attack, either on one target, or on a whole party.
	 *
	 * Note that this is a sealed class. The subclasses are [MoveTo], [Attack], and [JumpBack].
	 */
	@BitStruct(backwardCompatible = true)
	sealed class BreathAttack(

		/**
		 * The attacking/breathing combatant
		 */
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val attacker: CombatantState,

		/**
		 * The targets/victims of the attack.
		 */
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "combatants")
		val targets: Array<CombatantState>,

		/**
		 * The breath skill that `attacker` is using
		 */
		@BitField(id = 2)
		@ReferenceField(stable = true, label = "skills")
		val skill: ActiveSkill,

		/**
		 * When the attacker and/or target are player characters, and they have relevant reaction skills,
		 * this `reactionChallenge` will be non-null. These reaction skills will be applied if and only if this
		 * reaction challenge is passed. Furthermore, if needed, the damage calculation will be postponed until the
		 * outcome of the reaction challenge has been determined.
		 */
		@BitField(id = 3, optional = true)
		val reactionChallenge: ReactionChallenge?,

		/**
		 * The current value of [mardek.state.ingame.CampaignState.time]
		 */
		currentCampaignTime: Time,
	) : BattleStateMachine(), Move {

		/**
		 * The value of [mardek.state.ingame.CampaignState.time] when the battle transitioned to this state.
		 */
		@BitField(id = 4)
		val startTime = currentCampaignTime

		@Suppress("unused")
		constructor() : this(
			MonsterCombatantState(), emptyArray(),
			ActiveSkill(), null, Time.ZERO,
		)

		companion object {

			/**
			 * Determines the reaction challenge that the player should get, if any
			 */
			fun determineReactionChallenge(
				attacker: CombatantState,
				targets: Array<CombatantState>,
				context: BattleUpdateContext,
			): ReactionChallenge? {
				var primaryType: ReactionSkillType? = null

				if (targets.any { it.hasReactions(context, ReactionSkillType.RangedDefense) }) {
					primaryType = ReactionSkillType.RangedDefense
				}
				if (attacker.hasReactions(context, ReactionSkillType.RangedAttack)) {
					primaryType = ReactionSkillType.RangedAttack
				}

				return if (primaryType != null) ReactionChallenge(primaryType, context.campaignTime) else null
			}
		}

		/**
		 * The first phase of [BreathAttack]:
		 * - If the breath attack is melee, the attacker runs towards the target, but keeps a bit of distance.
		 * - If the breath attack is ranged, the attacker jumps to a position such that its mouth is near the centre
		 * of the screen.
		 */
		@BitStruct(backwardCompatible = true)
		class MoveTo(
			attacker: CombatantState, targets: Array<CombatantState>,
			skill: ActiveSkill, reactionChallenge: ReactionChallenge?,
			currentCampaignTime: Time,
		) : BreathAttack(attacker, targets, skill, reactionChallenge, currentCampaignTime) {

			/**
			 * Whether the attacker is at least halfway to the destination/breath position.
			 * This is useful for the rendering order/depth for 'melee' breath attacks.
			 */
			var halfWay = false

			/**
			 * Whether the attacker has reached the destination/breath position.
			 * The renderer should set this to `true` when the move-to animation is finished,
			 * after which the state should transition it to [Attack].
			 */
			var finished = false

			constructor(
				attacker: CombatantState, targets: Array<CombatantState>,
				skill: ActiveSkill, context: BattleUpdateContext,
			) : this(
				attacker, targets, skill,
				determineReactionChallenge(attacker, targets, context),
				context.campaignTime,
			)

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), emptyArray(),
				ActiveSkill(), null, Time.ZERO,
			)
		}

		/**
		 * The second phase of [BreathAttack]: this is the phase where the attacker breaths fire (or something similar)
		 * towards the target(s), and deals the damage.
		 */
		@BitStruct(backwardCompatible = true)
		class Attack(
			attacker: CombatantState, targets: Array<CombatantState>,
			skill: ActiveSkill, reactionChallenge: ReactionChallenge?,
			currentCampaignTime: Time,
		) : BreathAttack(attacker, targets, skill, reactionChallenge, currentCampaignTime) {

			/**
			 * The renderer should set this to `true` when the attacker is ~halfway the breath animation.
			 * When this is `true`, the state should deal the damage, and set [hasDealtDamage] to `true`.
			 */
			var canDealDamage = false

			/**
			 * The state should set this to `true` after it has 'processed' the damage calculation, and deducted the
			 * health from the target(s).
			 */
			@BitField(id = 0)
			var hasDealtDamage = false

			/**
			 * The renderer should set this to `true` after it has finished rendering the breath animation of the
			 * attacker. Once this is `true`, the state can be changed to [JumpBack].
			 */
			var finished = false

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), emptyArray(),
				ActiveSkill(), null, Time.ZERO,
			)

			/**
			 * Checks whether the reaction challenge is currently *pending*. While the reaction challenge is pending,
			 * the damage calculation must be postponed, since the outcome of the reaction challenge can influence the
			 * damage dealt.
			 *
			 * When there is no reaction challenge (e.g. there are no relevant reactions), this method will always
			 * return false.
			 */
			fun isReactionChallengePending(currentTime: Time) = this.reactionChallenge != null &&
					this.reactionChallenge.isPending(currentTime)
		}

		/**
		 * The attacker has finished its breath attack (and has dealt damage).
		 * During this state, the attacker will walk/jump back to its original position.
		 */
		@BitStruct(backwardCompatible = true)
		class JumpBack(
			attacker: CombatantState, targets: Array<CombatantState>,
			skill: ActiveSkill, reactionChallenge: ReactionChallenge?,
			currentCampaignTime: Time,
		) : BreathAttack(attacker, targets, skill, reactionChallenge, currentCampaignTime) {
			/**
			 * Whether the attacker is at least halfway on its way back to its original position.
			 * This is useful for the rendering order/depth.
			 */
			var halfWay = false

			/**
			 * Whether the attacker has reached its original position. The renderer should set this to `true` when
			 * the jump-back animation is finished, after which the state should transition it to
			 * [BattleStateMachine.NextTurn].
			 */
			var finished = false

			@Suppress("unused")
			private constructor() : this(
				MonsterCombatantState(), emptyArray(),
				ActiveSkill(), null, Time.ZERO,
			)
		}
	}

	/**
	 * A combatant is casting a ranged/magic skill
	 */
	@BitStruct(backwardCompatible = true)
	class CastSkill(

		/**
		 * The combatant that is using/casting the skill
		 */
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val caster: CombatantState,

		/**
		 * The combatants against which [caster] is using the skill
		 */
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "combatants")
		val targets: Array<CombatantState>,

		/**
		 * The skill that is being used/cast
		 */
		@BitField(id = 2)
		@ReferenceField(stable = true, label = "skills")
		val skill: ActiveSkill,

		/**
		 * When this skill changes the element of the caster (Elemental Shift),
		 * this field is the new element of the caster.
		 */
		@BitField(id = 3, optional = true)
		@ReferenceField(stable = true, label = "elements")
		val nextElement: Element?,

		context: BattleUpdateContext,
	) : BattleStateMachine(), Move, BitPostInit {

		/**
		 * The value of [mardek.state.ingame.CampaignState.time] the caster started casting the skill
		 */
		@BitField(id = 4)
		val startTime = context.campaignTime

		/**
		 * When the caster and/or target of this skill are player characters, and they have relevant reaction skills,
		 * this `reactionChallenge` will be non-null. These reaction skills will be applied if and only if this
		 * reaction challenge is passed. Furthermore, if needed, the damage calculation will be postponed until the
		 * outcome of the reaction challenge has been determined.
		 */
		@BitField(id = 5, optional = true)
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
		@BitField(id = 6)
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
		 * While casting magic skills, a small particle effect (depending on the element of the skill) is spawned
		 * continuously at the main hand of the caster. This field tracks when it was last spawned, to keep the rate
		 * at a stable 30 particles per second.
		 */
		var lastCastParticleSpawnTime = Time.ZERO

		/**
		 * All magic skills have a particle effect that is displayed at the position of the target(s).
		 * This field records the time at which that particle was spawned at the *first* target.
		 * - the particle for the second target will be spawned 250ms later
		 * - the particle for the third target will be spawned 500ms later
		 * - the particle for the fourth target will be spawned 750ms later
		 */
		var targetParticlesSpawnTime = Time.ZERO

		init {
			if (skill.targetType == SkillTargetType.Self || skill.targetType == SkillTargetType.Single) {
				if (targets.size > 1) throw IllegalArgumentException(
					"Illegal multi-target ${targets.contentToString()}for single-target skill ${skill.name}"
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
			reactionChallenge = if (primaryType != null) ReactionChallenge(primaryType, context.campaignTime) else null
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

		override fun hashCode() = caster.hashCode() + 13 * targets.contentHashCode() - 31 * skill.hashCode() +
				127 * Objects.hashCode(nextElement)

		/**
		 * Checks whether the reaction challenge is currently *pending*. While the reaction challenge is pending, the
		 * damage calculation must be postponed, since the outcome of the reaction challenge can influence the damage
		 * dealt.
		 *
		 * When there is no reaction challenge (e.g. there are no relevant reactions), this method will always return
		 * false.
		 */
		fun isReactionChallengePending(currentTime: Time) = reactionChallenge != null &&
				reactionChallenge!!.isPending(currentTime)

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

		/**
		 * The combatant that is throwing/using the item
		 */
		@BitField(id = 0)
		@ReferenceField(stable = false, label = "combatants")
		val thrower: CombatantState,

		/**
		 * The combatant that receives/consumes the item
		 */
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "combatants")
		val target: CombatantState,

		/**
		 * The item that is being thrown/used
		 */
		@BitField(id = 2)
		@ReferenceField(stable = true, label = "items")
		val item: Item,

		/**
		 * The current value of [mardek.state.ingame.CampaignState.time]
		 */
		currentCampaignTime: Time,
	) : BattleStateMachine(), Move {

		/**
		 * The value of [mardek.state.ingame.CampaignState.time] when [thrower] started throwing the item.
		 */
		@BitField(id = 3)
		val startTime = currentCampaignTime

		/**
		 * The renderer should set this field to `true` when the [item] has reached the [target].
		 * When this is `true`, the item should be consumed, and the state should be transitioned to [NextTurn].
		 */
		var canDrinkItem = false

		@Suppress("unused")
		private constructor() : this(
			MonsterCombatantState(), MonsterCombatantState(),
			Item(), Time.ZERO,
		)
	}

	/**
	 * The player ran away from the battle, so the game state should go back to the area state
	 */
	@BitStruct(backwardCompatible = true)
	class RanAway(

		/**
		 * The time at which the player decided to run away. This is also the moment where the fade-out starts.
		 *
		 * The player will exit the battle at `startedAt + FADE_OUT_DURATION`.
		 */
		@BitField(id = 0)
		val startedAt: Time,

		/**
		 * The player that ran away
		 */
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "combatants")
		val runningPlayer: PlayerCombatantState,
	) : BattleStateMachine() {

		@Suppress("unused")
		private constructor() : this(Time.ZERO, PlayerCombatantState())

		companion object {

			/**
			 * The duration of the flee/run-away fade-out
			 */
			val FADE_OUT_DURATION = 250.milliseconds
		}
	}

	/**
	 * All enemies have been slain, so the victory 'dance' should be shown. After a short while, the game should go
	 * to the battle loot menu.
	 */
	@BitStruct(backwardCompatible = true)
	class Victory(

		/**
		 * The current value of [mardek.state.ingame.CampaignState.time]
		 */
		currentCampaignTime: Time,
	) : BattleStateMachine() {

		/**
		 * The value of [mardek.state.ingame.CampaignState.time] when the battle reached this state
		 */
		@BitField(id = 0)
		val startTime = currentCampaignTime

		@Suppress("unused")
		private constructor() : this(Time.ZERO)

		companion object {

			/**
			 * The delay between [startTime] and the time at which the victory animation of the players starts.
			 *
			 * At this point, the game should also start playing the victory music.
			 */
			val DELAY_UNTIL_ANIMATION = 1500.milliseconds

			/**
			 * The delay between [startTime] and the time at which the "VICTORY!!" text starts fading in.
			 *
			 * At this point, the game should also remove the non-persistent status effects.
			 */
			val DELAY_UNTIL_TEXT = DELAY_UNTIL_ANIMATION + 500.milliseconds

			/**
			 * The duration of the "VICTORY!!" text fade-in, after [DELAY_UNTIL_TEXT]
			 */
			val VICTORY_TEXT_FADE_IN = 500.milliseconds

			/**
			 * The delay of the "VICTORY!!" text 'blink-out', after it is faded in
			 */
			val VICTORY_TEXT_BLINK_OUT = 250.milliseconds

			/**
			 * The amount of time between the end of the [VICTORY_TEXT_BLINK_OUT],
			 * and the start of the "VICTORY!!" text fade-out
			 */
			val VICTORY_TEXT_STABLE = 2000.milliseconds

			/**
			 * The duration of the "VICTORY!!" text fade-out, as well as the loot screen fade-in,
			 * starting after [VICTORY_TEXT_STABLE]
			 */
			val VICTORY_TEXT_FADE_OUT = 150.milliseconds

			/**
			 * The delay between [startTime], and the moment where the victory text starts losing its blink/right color
			 */
			val DELAY_UNTIL_TEXT_BLINK_OUT = DELAY_UNTIL_TEXT + VICTORY_TEXT_FADE_IN

			/**
			 * The delay between [startTime], and the moment where the loot screen fade-in starts
			 */
			val DELAY_UNTIL_LOOT_FADE_IN = DELAY_UNTIL_TEXT_BLINK_OUT + VICTORY_TEXT_BLINK_OUT + VICTORY_TEXT_STABLE

			/**
			 * The delay between [startTime], and the moment where the "VICTORY!!" text starts fading out
			 */
			val DELAY_UNTIL_TEXT_FADE_OUT = DELAY_UNTIL_LOOT_FADE_IN
		}
	}

	/**
	 * All players have fainted, so the battle screen should slowly fade black, after which the game should go to the
	 * game-over screen.
	 */
	@BitStruct(backwardCompatible = true)
	class GameOver(

		/**
		 * The time at which the last player fainted
		 */
		val startTime: Time,
	) : BattleStateMachine() {

		@Suppress("unused")
		private constructor() : this(Time.ZERO)

		/**
		 * When this returns `true` (5 seconds after the battle reached this state),
		 * the player should be taken to the 'Game Over' screen.
		 */
		fun shouldGoToGameOverMenu(currentTime: Time) = currentTime.virtualOffset(startTime) >= FADE_DURATION

		companion object {

			/**
			 * The fade-out time after the last player faints.
			 * The player should be taken to the 'Game Over' menu after the fade-out finishes.
			 */
			val FADE_DURATION = 5.seconds
		}
	}

	/**
	 * This interface is implemented by the [BattleStateMachine]s that are *moves*: e.g. [MeleeAttack] and [CastSkill]
	 * are moves, whereas [NextTurn] and [Victory] are not.
	 */
	sealed interface Move
}
