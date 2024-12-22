package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.*

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
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass?,
): Skill(name, description, element, masteryPoints) {

	@Suppress("unused")
	private constructor() : this(
		"", "", Element(), 0, 0, 0f, 0f,
		ArrayList(), ArrayList(), ArrayList(), HashSet(), HashSet(), 0f,
		0, 0, 0, null
	)

	fun getModifier(stat: CombatStat): Int {
		var total = 0
		for (modifier in statModifiers) {
			if (modifier.stat == stat) total += modifier.adder
		}
		return total
	}
}
