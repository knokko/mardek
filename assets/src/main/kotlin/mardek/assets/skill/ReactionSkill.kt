package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class ReactionSkill(
	@BitField(ordering = 0)
	val type: ReactionSkillType,

	@BitField(ordering = 1, optional = true)
	@ReferenceField(stable = false, label = "classes")
	val skillClass: SkillClass?,
): Skill() {
}
