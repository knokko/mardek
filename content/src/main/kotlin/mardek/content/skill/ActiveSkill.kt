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

@BitStruct(backwardCompatible = true)
class ActiveSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,

	@BitField(id = 0)
	val mode: ActiveSkillMode,

	@BitField(id = 1)
	val targetType: SkillTargetType,

	@BitField(id = 2, optional = true)
	val damage: SkillDamage?,

	@BitField(id = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 255)
	val accuracy: Int,

	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 1000)
	val manaCost: Int,

	@BitField(id = 5)
	val isHealing: Boolean,

	@BitField(id = 6)
	val isBreath: Boolean,

	@BitField(id = 7)
	val isBuff: Boolean,

	@BitField(id = 8)
	@FloatField(expectMultipleOf = 0.1)
	val healthDrain: Float,

	@BitField(id = 9)
	val statModifiers: ArrayList<StatModifierRange>,

	@BitField(id = 10)
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(id = 11)
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(id = 12)
	@FloatField(expectMultipleOf = 0.25)
	val revive: Float,

	@BitField(id = 13, optional = true)
	@ReferenceField(stable = false, label = "particles")
	val particleEffect: ParticleEffect?,

	@BitField(id = 14, optional = true)
	@ReferenceField(stable = false, label = "sound effects")
	val soundEffect: SoundEffect?,

	@BitField(id = 15, optional = true)
	val animation: String?,

	@BitField(id = 16)
	val combatRequirement: SkillCombatRequirement,

	/**
	 * Damage/sound delay, only used by needleflare (it's 20). I have no clue what the time unit is.
	 */
	@BitField(id = 17)
	@IntegerField(expectUniform = false, minValue = 0)
	val delay: Int,

	/**
	 * I'm not completely sure what this does. It's true for inferno, thunderstorm, earthquake,
	 * tsunami, galaxy burst, and bloodmoon
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

	@BitField(id = 21, optional = true)
	val rawSongPower: String?,

	@BitField(id = 22)
	val changeElement: Boolean,
): Skill(name, description, element, masteryPoints) {

	constructor() : this(
		"", "", Element(), 0, ActiveSkillMode.Melee, SkillTargetType.Single, null,
		0, 0, false, false, false, 0f, ArrayList(), ArrayList(),
		ArrayList(), 0f, null, null, null, SkillCombatRequirement.OutsideCombat,
		0, false, false, false, null, false
	)

	fun isPositive() = isHealing || isBuff
}
