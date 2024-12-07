package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.skill.Skill

@BitStruct(backwardCompatible = false)
class EquipmentProperties(
	@BitField(ordering = 0, optional = true)
	val onlyUser: String?, // TODO Use playable character reference

	@BitField(ordering = 1)
	@ReferenceField(stable = false, label = "skills")
	val skills: ArrayList<Skill>,
	// TODO Skills and effects
) {
}
