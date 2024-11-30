package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element
import mardek.assets.combat.PossibleStatusEffect
import mardek.assets.combat.StatModifierRange

@BitStruct(backwardCompatible = false)
class ActiveSkill(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val description: String,

	@BitField(ordering = 2)
	val mode: ActiveSkillMode,

	@BitField(ordering = 3)
	val targetType: SkillTargetType,

	@BitField(ordering = 4)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 5, optional = true)
	val damage: SkillDamage?,

	@BitField(ordering = 6)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val masteryPoints: Int,

	@BitField(ordering = 7)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	val accuracy: Int,

	@BitField(ordering = 8)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 1000)
	val manaCost: Int,

	@BitField(ordering = 9)
	val isHealing: Boolean,

	@BitField(ordering = 10)
	val isBreath: Boolean,

	@BitField(ordering = 11)
	val isBuff: Boolean,

	@BitField(ordering = 12)
	val drainsBlood: Boolean,

	@BitField(ordering = 13)
	@CollectionField
	val statModifiers: ArrayList<StatModifierRange>,

	@BitField(ordering = 14)
	@CollectionField
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 15)
	@CollectionField
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 16)
	@FloatField(expectMultipleOf = 0.25)
	val revive: Float,

	@BitField(ordering = 17, optional = true)
	val particleEffect: String?,

	@BitField(ordering = 18, optional = true)
	val soundEffect: String?,

	@BitField(ordering = 19, optional = true)
	val animation: String?,

	@BitField(ordering = 20)
	val combatRequirement: SkillCombatRequirement,

	/**
	 * Damage/sound delay, only used by needleflare (it's 20). I have no clue what the time unit is.
	 */
	@BitField(ordering = 21)
	@IntegerField(expectUniform = false, minValue = 0)
	val delay: Int,

	/**
	 * I'm not completely sure what this does. It's true for inferno, thunderstorm, earthquake,
	 * tsunami, galaxy burst, and bloodmoon
	 */
	@BitField(ordering = 23)
	val allParticleEffects: Boolean,

	/**
	 * I'm not completely sure what this does. It's true for inferno, thunderstorm, earthquake,
	 * tsunami, and bloodmoon
	 */
	@BitField(ordering = 24)
	val centered: Boolean,

	/**
	 * I'm not completely sure what this does. It's only **false** for inferno, thunderstorm,
	 * earthquake, and tsunami.
	 */
	@BitField(ordering = 25)
	val arena: Boolean,

	@BitField(ordering = 26, optional = true)
	val rawSongPower: String?,
): Skill() {

	override fun toString() = name
}
