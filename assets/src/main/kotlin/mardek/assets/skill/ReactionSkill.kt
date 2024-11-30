package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
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
	@ReferenceField(stable = false, label = "classes")
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
	@CollectionField
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	@BitField(ordering = 10)
	@CollectionField
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 11)
	@CollectionField
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 12)
	@CollectionField
	val effectiveAgainst: ArrayList<RaceDamageBonus>,

	@BitField(ordering = 13)
	val smitePlus: Boolean,

	@BitField(ordering = 14)
	val soulStrike: Boolean,

	@BitField(ordering = 15)
	val survivor: Boolean,
): Skill(name, description, element, masteryPoints) {
}
