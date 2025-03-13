package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.combat.CreatureTypeBonus
import mardek.content.combat.Element
import mardek.content.combat.ElementalDamageBonus
import mardek.content.combat.PossibleStatusEffect

@BitStruct(backwardCompatible = true)
class ReactionSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,

	@BitField(id = 0)
	val type: ReactionSkillType,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val enablePoints: Int,

	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass?,

	@BitField(id = 3)
	@IntegerField(expectUniform = false)
	val addFlatDamage: Int,

	@BitField(id = 4)
	@FloatField(expectMultipleOf = 0.1)
	val addDamageFraction: Float,

	@BitField(id = 5)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val addCritChance: Int,

	@BitField(id = 6)
	@IntegerField(expectUniform = false)
	val addAccuracy: Int,

	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.1)
	val drainHp: Float,

	@BitField(id = 8)
	@FloatField(expectMultipleOf = 0.1)
	val absorbMp: Float,

	@BitField(id = 9)
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	@BitField(id = 10)
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(id = 11)
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(id = 12)
	val effectiveAgainst: ArrayList<CreatureTypeBonus>,

	@BitField(id = 13)
	val smitePlus: Boolean,

	@BitField(id = 14)
	val soulStrike: Boolean,

	@BitField(id = 15)
	val survivor: Boolean,
): Skill(name, description, element, masteryPoints) {

	@Suppress("unused")
	private constructor() : this(
		"", "", Element(), 0, ReactionSkillType.RangedDefense, 0, null,
		0, 0f, 0, 0, 0f, 0f, ArrayList(0),
		ArrayList(0), ArrayList(0), ArrayList(0), false, false, false
	)
}
