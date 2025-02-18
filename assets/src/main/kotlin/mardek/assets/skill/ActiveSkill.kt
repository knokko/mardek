package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import mardek.assets.combat.Element
import mardek.assets.combat.PossibleStatusEffect
import mardek.assets.combat.StatModifierRange

@BitStruct(backwardCompatible = false)
class ActiveSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,

	@BitField(ordering = 0)
	val mode: ActiveSkillMode,

	@BitField(ordering = 1)
	val targetType: SkillTargetType,

	@BitField(ordering = 2, optional = true)
	val damage: SkillDamage?,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 255)
	val accuracy: Int,

	@BitField(ordering = 4)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 1000)
	val manaCost: Int,

	@BitField(ordering = 5)
	val isHealing: Boolean,

	@BitField(ordering = 6)
	val isBreath: Boolean,

	@BitField(ordering = 7)
	val isBuff: Boolean,

	@BitField(ordering = 8)
	val drainsBlood: Boolean,

	@BitField(ordering = 9)
	val statModifiers: ArrayList<StatModifierRange>,

	@BitField(ordering = 10)
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 11)
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 12)
	@FloatField(expectMultipleOf = 0.25)
	val revive: Float,

	@BitField(ordering = 13, optional = true)
	val particleEffect: String?,

	@BitField(ordering = 14, optional = true)
	val soundEffect: String?,

	@BitField(ordering = 15, optional = true)
	val animation: String?,

	@BitField(ordering = 16)
	val combatRequirement: SkillCombatRequirement,

	/**
	 * Damage/sound delay, only used by needleflare (it's 20). I have no clue what the time unit is.
	 */
	@BitField(ordering = 17)
	@IntegerField(expectUniform = false, minValue = 0)
	val delay: Int,

	/**
	 * I'm not completely sure what this does. It's true for inferno, thunderstorm, earthquake,
	 * tsunami, galaxy burst, and bloodmoon
	 */
	@BitField(ordering = 18)
	val allParticleEffects: Boolean,

	/**
	 * I'm not completely sure what this does. It's true for inferno, thunderstorm, earthquake,
	 * tsunami, and bloodmoon
	 */
	@BitField(ordering = 19)
	val centered: Boolean,

	/**
	 * I'm not completely sure what this does. It's only **false** for inferno, thunderstorm,
	 * earthquake, and tsunami.
	 */
	@BitField(ordering = 20)
	val arena: Boolean,

	@BitField(ordering = 21, optional = true)
	val rawSongPower: String?,
): Skill(name, description, element, masteryPoints) {

	internal constructor() : this(
		"", "", Element(), 0, ActiveSkillMode.Melee, SkillTargetType.Single, null,
		0, 0, false, false, false, false, ArrayList(), ArrayList(),
		ArrayList(), 0f, null, null, null, SkillCombatRequirement.OutsideCombat,
		0, false, false, false, null
	)
}
