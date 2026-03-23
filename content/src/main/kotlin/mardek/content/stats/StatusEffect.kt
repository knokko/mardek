package mardek.content.stats

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.particle.ParticleEffect
import mardek.content.particle.ParticleEmitter
import mardek.content.sprite.BcSprite
import java.util.*
import kotlin.collections.ArrayList

/**
 * Status effects are temporary effects that combatants can have. There are both positive status effects
 * (e.g. Regeneration) and negative status effects (e.g. Poison).
 *
 * Status effects can do many things, for instance:
 * - Dealing damage every turn
 * - Reducing incoming damage
 * - Prevent the combatant from doing melee attacks
 */
@BitStruct(backwardCompatible = true)
class StatusEffect(

	/**
	 * The raw 3-letter name of the status effect, as imported from Flash (e.g. PSN for poison)
	 */
	@BitField(id = 0)
	val flashName: String,

	/**
	 * The nice/display name of the status effect (e.g. Poison)
	 */
	@BitField(id = 1, optional = true)
	val niceName: String?,

	/**
	 * Whether the status effect counts as a positive status effect. This determines whether it is 'cured' by e.g.
	 * Remedies or the Remove Evil skill.
	 */
	@BitField(id = 2)
	val isPositive: Boolean,

	/**
	 * Whether the combatant will lose the status effect after combat. This is true for all positive status effects,
	 * as well as some negative status effects (e.g. Sleep).
	 */
	@BitField(id = 3)
	val disappearsAfterCombat: Boolean,

	/**
	 * This will be non-null if and only if this status effect deals damage every turn (or heals every turn).
	 */
	@BitField(id = 4, optional = true)
	val damagePerTurn: TurnDamage? = null,

	/**
	 * This will be non-null if and only if this status effect deals damage while the combatant is walking outside combat
	 * (in vanilla MARDEK, this is only used by Poison)
	 */
	@BitField(id = 5, optional = true)
	val damageWhileWalking: WalkDamage? = null,

	/**
	 * The damage dealt by melee attacks of the combatant is multiplied by `1f + meleeDamageModifier` (used by Berserk)
	 */
	@BitField(id = 6)
	@FloatField(expectMultipleOf = 0.25)
	val meleeDamageModifier: Float = 0f,

	/**
	 * The damage dealt by melee attacks against the combatant is multiplied by `1f - meleeDamageReduction`
	 * (used by P. Shield)
	 */
	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.25)
	val meleeDamageReduction: Float = 0f,

	/**
	 * The damage dealt by magic/ranged attacks against the combatant is multiplied by `1f - rangedDamageReduction`
	 * (used by M. Shield)
	 */
	@BitField(id = 8)
	@FloatField(expectMultipleOf = 0.25)
	val rangedDamageReduction: Float = 0f,

	/**
	 * When non-null, this status effect will nullify/absorb attacks of this element, after which the status effect
	 * is removed.
	 */
	@BitField(id = 9, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val nullifiesElement: Element? = null,

	/**
	 * When non-null, this is an elemental shell status effect (e.g. Pyro Shell). TODO CHAP3 Figure out what it does
	 */
	@BitField(id = 10, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val elementShell: Element? = null,

	/**
	 * When the combatant tries to perform a melee attack, the chance to hit is reduced by `missChance`%
	 */
	@BitField(id = 11)
	@IntegerField(expectUniform = false, minValue = 0)
	val missChance: Int = 0,

	/**
	 * Whether this status effect prevents the combatant from using ranged/magic skills (Silence and Curse)
	 */
	@BitField(id = 12)
	val blocksRangedSkills: Boolean = false,

	/**
	 * Whether this status effect prevents the combatant from using melee skills (Numbness and Curse)
	 */
	@BitField(id = 13)
	val blocksMeleeSkills: Boolean = false,

	/**
	 * Whether the status effect prevents the combatant from using basic attacks (only used by Numbness)
	 */
	@BitField(id = 14)
	val blocksBasicAttacks: Boolean = false,

	/**
	 * Whether this status effect confuses the combatant (only used by the Confusion effect)
	 */
	@BitField(id = 15)
	val isConfusing: Boolean = false,

	/**
	 * Whether this status effect zombifies the combatant (only used by the Zombify effect)
	 */
	@BitField(id = 16)
	val isZombie: Boolean = false,

	/**
	 * The number of extra turns that the combatant gets each round (1 for Haste, 0 for every other effect)
	 */
	@BitField(id = 17)
	@IntegerField(expectUniform = false, minValue = 0)
	val extraTurns: Int = 0,

	/**
	 * When true, the combatant will use a basic attack against a random enemy each turn (only used by Berserk).
	 */
	@BitField(id = 18)
	val isReckless: Boolean = false,

	/**
	 * Whether this status effect allows the combatant to breathe underwater (only used by Aqualung)
	 */
	@BitField(id = 19)
	val canWaterBreathe: Boolean = false,

	/**
	 * Only used by Barskin TODO CHAP3 Figure it out
	 */
	@BitField(id = 20)
	val hasBarskin: Boolean = false,

	/**
	 * Only used by Astral Form TODO CHAP3 Figure it out
	 */
	@BitField(id = 21)
	val isAstralForm: Boolean = false,

	/**
	 * The [StatModifier]s of this status effect, which I might want to use for Barskin someday.
	 */
	@BitField(id = 22)
	val statModifiers: ArrayList<StatModifier> = ArrayList(0),

	/**
	 * The elemental resistances granted by this status effect (only used by Astral Form)
	 */
	@BitField(id = 23)
	val resistances: Resistances = Resistances(),

	/**
	 * When non-null, there is a chance that this combatant will skip each turn (this is decided right before the
	 * turn of the combatant). This is used by Paralysis (40%) and Sleep (100%)
	 */
	@BitField(id = 24, optional = true)
	val skipTurn: SkipTurn? = null,

	/**
	 * The chance that this status effect is removed at the beginning of each turn. This is used by Sleep and
	 * Confusion.
	 */
	@BitField(id = 25)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val disappearChancePerTurn: Int = 0,

	/**
	 * The chance that this status effect is removed when the combatant is hit by a melee attack. This is used by
	 * Sleep and Confusion (both 100%)
	 */
	@BitField(id = 26)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val disappearAfterHitChance: Int = 0,

	/**
	 * The icon of this status effect, which is rendered near the health bar of the combatant.
	 */
	@BitField(id = 27)
	val icon: BcSprite,

	/**
	 * When the name of this status effect is rendered, the text normally has a vertical gradient. This field is the
	 * color at the middle of the gradient.
	 */
	@BitField(id = 28)
	@IntegerField(expectUniform = true)
	val innerTextColor: Int,

	/**
	 * When the name of this status effect is rendered, the text normally has a vertical gradient. This field is the
	 * color at the top and bottom of the gradient.
	 */
	@BitField(id = 29)
	@IntegerField(expectUniform = true)
	val outerTextColor: Int,

	/**
	 * Describes the particles that should be emitted from each combatant with this status effect.
	 *
	 * For instance, all poisoned combatants keep emitting these green poison bubble particles.
	 */
	@BitField(id = 30)
	val particleEmitters: Array<ParticleEmitter> = emptyArray(),

	/**
	 * This is yet another name for the particle effect. It is typically longer than [flashName], but shorter than
	 * [niceName].
	 *
	 * For instance, the paralysis effect has `flashName = PAR`, `shortName = Stun`, and `niceName = Paralysis`.
	 */
	@BitField(id = 31)
	val shortName: String = niceName ?: flashName,

	/**
	 * The unique ID of this status effect, which is used for (de)serialization
	 */
	@Suppress("unused")
	@BitField(id = 32)
	@StableReferenceFieldId
	val id: UUID,
) {

	constructor() : this(
		"", null, false, false,
		icon = BcSprite(), innerTextColor = 0, outerTextColor = 0, id = UUID.randomUUID(),
	)

	override fun toString() = niceName ?: flashName

	/**
	 * Determines by how much this status effect will increase the [stat] value of the combatant. This simply sums up
	 * the [statModifiers] for [stat].
	 */
	fun getModifier(stat: CombatStat) = statModifiers.sumOf { if (it.stat == stat) it.adder else 0 }

	/**
	 * Describes how much HP the combatant loses each turn, as well as the associated element and particle effect.
	 */
	@BitStruct(backwardCompatible = true)
	class TurnDamage(

		/**
		 * The fraction of the maximum HP that the combatant loses each turn,
		 * basically `damagePerTurn = hpFraction * maxHealth`.
		 *
		 * When this is negative (e.g. Regeneration), the combatant will be healed each turn.
		 */
		@BitField(id = 0)
		@FloatField(expectMultipleOf = 0.05)
		val hpFraction: Float,

		/**
		 * The element whose sprite should be rendered behind the damage indicator/number.
		 *
		 * **Note that it doesn't matter how much resistance the combatant has against this status effect!**
		 * Status effect damage ignores all elemental resistances!
		 */
		@BitField(id = 1)
		@ReferenceField(stable = false, label = "elements")
		val element: Element,

		/**
		 * The particle effect that should be played each time the combatant is damaged (or healed) by this status
		 * effect.
		 */
		@BitField(id = 2)
		@ReferenceField(stable = false, label = "particles")
		val particleEffect: ParticleEffect,

		/**
		 * The color to which the combatant should blink each time it is damaged or healed by this status effect.
		 */
		@BitField(id = 3)
		@IntegerField(expectUniform = true)
		val blinkColor: Int,
	) {
		@Suppress("unused")
		private constructor() : this(0f, Element(), ParticleEffect(), 0)
	}

	/**
	 * Describes how much damage the character takes while walking through an area, and how often.
	 * This is only applicable to players, since monster combatants don't walk in areas.
	 */
	@BitStruct(backwardCompatible = true)
	class WalkDamage(

		/**
		 * The combatant will take damage every `period` steps
		 */
		@BitField(id = 0)
		@IntegerField(expectUniform = false, minValue = 1)
		val period: Int,

		/**
		 * The fraction of its maximum HP that the character loses when it takes damage,
		 * basically `damagePerPeriodSteps = hpFraction * maximumHP`.
		 */
		@BitField(id = 1)
		@FloatField(expectMultipleOf = 0.01)
		val hpFraction: Float,

		/**
		 * The color to which the character should blink each time it is damaged or healed by this status effect.
		 */
		@BitField(id = 2)
		@IntegerField(expectUniform = true)
		val blinkColor: Int,
	) {
		@Suppress("unused")
		private constructor() : this(0, 0f, 0)
	}

	/**
	 * When a combatant starts a turn while having this status effect, this class describes the probability that the
	 * turn is skipped, as well as the associated (particle) effects.
	 */
	@BitStruct(backwardCompatible = true)
	class SkipTurn(

		/**
		 * The chance that the turn will be skipped, this is e.g. 100% for Sleep and 40% for Paralysis.
		 */
		@BitField(id = 0)
		@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
		val chance: Int,

		/**
		 * The color to which the combatant should briefly blink when its turn is skipped due to this status effect.
		 * This is yellow for Paralysis, but unused by Sleep.
		 */
		@BitField(id = 1)
		@IntegerField(expectUniform = true)
		val blinkColor: Int,

		/**
		 * The particle effect that should be played when the combatant skips a turn due to this status effect.
		 */
		@BitField(id = 2, optional = true)
		@ReferenceField(stable = false, label = "particles")
		val particleEffect: ParticleEffect?,
	) {
		@Suppress("unused")
		private constructor() : this(0, 0, null)
	}
}
