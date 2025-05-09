package mardek.content.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FloatField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.content.stats.*

@BitStruct(backwardCompatible = true)
class PassiveSkill(
	name: String,
	description: String,
	element: Element,
	masteryPoints: Int,

	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 0)
	val enablePoints: Int,

	@BitField(id = 1)
	@FloatField(expectMultipleOf = 0.05)
	val hpModifier: Float,

	@BitField(id = 2)
	@FloatField(expectMultipleOf = 0.05)
	val mpModifier: Float,

	@BitField(id = 3)
	val statModifiers: ArrayList<StatModifier>,

	@BitField(id = 4)
	val resistances: Resistances,

	@BitField(id = 5)
	@ReferenceField(stable = false, label = "status effects")
	val autoEffects: HashSet<StatusEffect>,

	@BitField(id = 6)
	@ReferenceField(stable = false, label = "status effects")
	val sosEffects: HashSet<StatusEffect>,

	@BitField(id = 7)
	@FloatField(expectMultipleOf = 0.05)
	val experienceModifier: Float,

	@BitField(id = 8)
	@IntegerField(expectUniform = false)
	val masteryModifier: Int,

	@BitField(id = 9)
	@IntegerField(expectUniform = false)
	val goldModifier: Int,

	@BitField(id = 10)
	@IntegerField(expectUniform = false)
	val addLootChance: Int,

	@BitField(id = 11, optional = true)
	@ReferenceField(stable = false, label = "skill classes")
	val skillClass: SkillClass?,
): Skill(name, description, element, masteryPoints) {

	@Suppress("unused")
	private constructor() : this(
		"", "", Element(), 0, 0, 0f, 0f,
		ArrayList(), Resistances(), HashSet(), HashSet(), 0f,
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
