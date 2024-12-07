package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element
import mardek.assets.combat.PossibleStatusEffect
import mardek.assets.combat.StatModifier
import mardek.assets.combat.StatusEffect

@BitStruct(backwardCompatible = false)
class PassiveSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,

	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val enablePoints: Int,

	@BitField(ordering = 1)
	@FloatField(expectMultipleOf = 0.05)
	val hpModifier: Float,

	@BitField(ordering = 2)
	@FloatField(expectMultipleOf = 0.05)
	val mpModifier: Float,

	@BitField(ordering = 3)
	val statModifiers: ArrayList<StatModifier>,

	@BitField(ordering = 4)
	val elementalResistances: ArrayList<ElementalDamageBonus>,

	@BitField(ordering = 5)
	val statusResistances: ArrayList<PossibleStatusEffect>,

	@BitField(ordering = 6)
	@ReferenceField(stable = false, label = "status effects")
	val autoEffects: HashSet<StatusEffect>,

	@BitField(ordering = 7)
	@ReferenceField(stable = false, label = "status effects")
	val sosEffects: HashSet<StatusEffect>,

	@BitField(ordering = 8)
	@FloatField(expectMultipleOf = 0.05)
	val experienceModifier: Float,

	@BitField(ordering = 9)
	@IntegerField(expectUniform = false)
	val masteryModifier: Int,

	@BitField(ordering = 10)
	@IntegerField(expectUniform = false)
	val goldModifier: Int,

	@BitField(ordering = 11)
	@IntegerField(expectUniform = false)
	val addLootChance: Int,

	@BitField(ordering = 12, optional = true)
	@ReferenceField(stable = false, label = "classes")
	val skillClass: SkillClass?,
): Skill(name, description, element, masteryPoints) {
}
