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
		// TODO Fix orderings
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 0)
	val description: String,

	@BitField(ordering = 0)
	val type: ReactionSkillType,

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = -1)
	val masteryPoints: Int,

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val enablePoints: Int,

	@BitField(ordering = 1, optional = true)
	@ReferenceField(stable = false, label = "classes")
	val skillClass: SkillClass?,

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false)
	val addFlatDamage: Int,

	@BitField(ordering = 0)
	@FloatField(expectMultipleOf = 0.1)
	val addDamageFraction: Float,

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0, maxValue = 100)
	val addCritChance: Int,

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false)
	val addAccuracy: Int,

	@BitField(ordering = 0)
	@FloatField(expectMultipleOf = 0.1)
	val drainHp: Float,

	@BitField(ordering = 0)
	@FloatField(expectMultipleOf = 0.1)
	val absorbMp: Float,

	@BitField(ordering = 0)
	@CollectionField
	val elementalBonuses: ArrayList<ElementalDamageBonus>,

	@BitField(ordering = 0)
	@CollectionField
	val addStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 0)
	@CollectionField
	val removeStatusEffects: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 0)
	@CollectionField
	val effectiveAgainst: ArrayList<RaceDamageBonus>,

	@BitField(ordering = 0)
	val smitePlus: Boolean,

	@BitField(ordering = 0)
	val soulStrike: Boolean,

	@BitField(ordering = 0)
	val survivor: Boolean,
): Skill() {
}
