package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class PassiveSkill(
	@BitField(ordering = 0, optional = true)
	@ReferenceField(stable = false, label = "classes")
	val skillClass: SkillClass?,
): Skill() {
}
