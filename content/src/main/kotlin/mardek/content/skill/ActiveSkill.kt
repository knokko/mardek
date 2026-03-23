package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.audio.SoundEffect
import mardek.content.particle.ParticleEffect
import mardek.content.stats.Element
import mardek.content.stats.PossibleStatusEffect
import mardek.content.stats.StatModifierRange
import java.util.UUID

/**
 * Represents an active skill that combatants can use/cast during a battle (e.g. Sunder, Pyromagia, and Recover).
 */
@BitStruct(backwardCompatible = true)
class ActiveSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,
	id: UUID,

	/**
	 * Whether the skill is melee (e.g. Sunder, but *not* Pyromagia)
	 */
	@BitField(id = 0)
	val isMelee: Boolean,

	/**
	 * The target type of this skill, which determines on which combatants this skill can be used, and on how many at
	 * the same time. Some skills (e.g. Recover) can only be used on the caster, whereas other skills (e.g. Pyromagia)
	 * can be used on anyone, and even on multiple targets at the same time.
	 */
	@BitField(id = 1)
	var targetType: SkillTargetType,

	/**
	 * When a skill deals damage or heals, this field influences the amount of damage/healing. Most skills in the game
	 * will have `damage != null`, but not all of them (e.g. Remote Taint).
	 */
	@BitField(id = 2, optional = true)
	val damage: SkillDamage?,

	/**
	 * The base accuracy, or hit chance, of this skill (%). Note that the final hit chance may
	 * also depend on other factors (e.g. blindness or enemy evasion).
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 255, commonValues=[100, 255])
	val accuracy: Int,

	/**
	 * The amount of MP that is needed to cast this skill. When the skill is cast, this amount is deducted from the
	 * MP of the caster.
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 1000)
	val manaCost: Int,

	/**
	 * Whether this skill is a healing skill. When this is `true`, [damage] should be non-null, and the computed
	 * amount of damage will be added to the target HP rather than removed from it.
	 *
	 * Furthermore, healing skills will ignore the defense and resistances of the target, unless the target takes
	 * damage from healing (e.g. undead).
	 */
	@BitField(id = 5)
	val isHealing: Boolean,

	/**
	 * Whether this skill is a breath attack. Breath attacks have different positioning than regular attacks and skills.
	 * - Melee breath attacks have slightly longer range than normal melee attacks, so the attacker will keep a bit more
	 * distance from the target.
	 * - During a ranged breath attack, the attacker will move to a position such that its `BreathSource` is at the
	 * `BreathCentre` of the screen (near the middle).
	 */
	@BitField(id = 6)
	val isBreath: Boolean,

	/**
	 * Whether this skill is a buff/improvement skill. Such skills ignore protections like Null Earth. Examples of
	 * buff skills are Remove Taint and Shield.
	 */
	@BitField(id = 7)
	val isBuff: Boolean,

	/**
	 * When this skill deals `X` damage against the target, the caster/attacker regains `X * healthDrain` HP.
	 */
	@BitField(id = 8)
	@FloatField(expectMultipleOf = 0.1)
	val healthDrain: Float,

	/**
	 * The target stats that can be increased or decreased. This is used by e.g. Perforate to reduce the DEF of the
	 * target.
	 */
	@BitField(id = 9)
	val statModifiers: ArrayList<StatModifierRange>,

	/**
	 * The status effects that may be given to the target. This is used by e.g. Infect to poison the target.
	 */
	@BitField(id = 10)
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * The status effects that may be removed from the target. This is used by e.g. Remote Taint to cure the target
	 * from poison.
	 */
	@BitField(id = 11)
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	/**
	 * For almost all skills, this will be `0f`, which means that the skill cannot be used on fainted targets.
	 * When this is non-zero, the skill can only be used on fainted targets, and will restore `revive * maxHP` health
	 * to such targets.
	 *
	 * As far as I know, this is currently only used by Resurrect and Zombify.
	 */
	@BitField(id = 12)
	@FloatField(expectMultipleOf = 0.25)
	val revive: Float,

	/**
	 * The particle effect of this skill, which will be shown when the skill is used. It also determines the sound
	 * effect of the skill.
	 *
	 * Almost all skills (especially ranged skills) have a particle effect, but there are exceptions (e.g. Huff Puff).
	 */
	@BitField(id = 13, optional = true)
	@ReferenceField(stable = false, label = "particles")
	val particleEffect: ParticleEffect?,

	/**
	 * The damage sound effect of the skill, or `null`.
	 *
	 * When this is `null`, the damage sound of [particleEffect] will be used. If that is also `null` and this is a
	 * melee skill, the damage sound of the used weapon will be used. If that is also `null`, the default hit/punch
	 * sound effect will be used.
	 */
	@BitField(id = 14, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val damageSound: SoundEffect?,

	/**
	 * This field is almost always `null`, which means that the default casting or attack animation of the
	 * caster/attacker will be played. When this field is non-null, the animation with this name will be played
	 * instead.
	 *
	 * As far as I know, this is only used by Huff Puff.
	 */
	@BitField(id = 15, optional = true)
	val animation: String?,

	/**
	 * Whether this skill can only be used in combat, only out of combat, or both
	 */
	@BitField(id = 16)
	val combatRequirement: SkillCombatRequirement,

	/**
	 * Damage/sound delay, only used by needleflare (it's 20). I suspect this means a delay of 20 frames (0.67
	 * seconds), but that is a problem for chapter 3.
	 */
	@BitField(id = 17)
	@IntegerField(expectUniform = false, minValue = 0)
	val delay: Int,

	/**
	 * I'm not completely sure what this does. It's true for inferno, thunderstorm, earthquake,
	 * tsunami, galaxy burst, and bloodmoon.
	 */
	@BitField(id = 18)
	val allParticleEffects: Boolean,

	/**
	 * I'm not completely sure what this does. It's true for inferno, thunderstorm, earthquake,
	 * tsunami, and bloodmoon
	 */
	@BitField(id = 19)
	val centered: Boolean,

	/**
	 * I'm not completely sure what this does. It's only **false** for inferno, thunderstorm,
	 * earthquake, and tsunami.
	 */
	@BitField(id = 20)
	val arena: Boolean,

	/**
	 * This is only used for the song skills of Elwyen. Figuring this out is a problem for chapter 3.
	 */
	@BitField(id = 21, optional = true)
	val rawSongPower: String?,

	/**
	 * This field is almost always `false`. But, when it is `true`, the caster will change its element to the element
	 * of this skill. This is only used for the Elemental Shift skill.
	 */
	@BitField(id = 22)
	val changeElement: Boolean,
): Skill(name, description, element, masteryPoints, id) {

	constructor() : this(
		"", "", Element(), 0, UUID.randomUUID(), false,
		SkillTargetType.Single, null, 0, 0, false,
		false, false, 0f, ArrayList(), ArrayList(),
		ArrayList(), 0f, null, null, null,
		SkillCombatRequirement.OutsideCombat, 0, false, false,
		false, null, false,
	)

	/**
	 * Whether this skill is supposed to be a 'positive' skill. Such skills should ignore defenses, unless the target
	 * reverts healing (e.g. because its undead).
	 */
	fun isPositive() = isHealing || isBuff

	override fun hashCode() = id.hashCode()

	override fun equals(other: Any?) = other is ActiveSkill && id == other.id
}
