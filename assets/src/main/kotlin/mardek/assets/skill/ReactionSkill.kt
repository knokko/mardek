package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element
import mardek.assets.combat.PossibleStatusEffect

@BitStruct(backwardCompatible = false)
class ReactionSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,

	@BitField(ordering = 0)
	val type: ReactionSkillType,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val enablePoints: Int,

	@BitField(ordering = 2, optional = true)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass?,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false)
	val addFlatDamage: Int,

	@BitField(ordering = 4)
	@FloatField(expectMultipleOf = 0.1)
	val addDamageFraction: Float,

	@BitField(ordering = 5)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val addCritChance: Int,

	@BitField(ordering = 6)
	@IntegerField(expectUniform = false)
	val addAccuracy: Int,

	@BitField(ordering = 7)
	@FloatField(expectMultipleOf = 0.1)
	val drainHp: Float,

	@BitField(ordering = 8)
	@FloatField(expectMultipleOf = 0.1)
	val absorbMp: Float,

	@BitField(ordering = 9)
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	@BitField(ordering = 10)
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 11)
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 12)
	val effectiveAgainst: ArrayList<RaceDamageBonus>,

	@BitField(ordering = 13)
	val smitePlus: Boolean,

	@BitField(ordering = 14)
	val soulStrike: Boolean,

	@BitField(ordering = 15)
	val survivor: Boolean,
): Skill(name, description, element, masteryPoints) {

	@Suppress("unused")
	private constructor() : this(
		"", "", Element(), 0, ReactionSkillType.RangedDefense, 0, null,
		0, 0f, 0, 0, 0f, 0f, ArrayList(0),
		ArrayList(0), ArrayList(0), ArrayList(0), false, false, false
	)
}
