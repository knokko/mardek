package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField

@BitStruct(backwardCompatible = false)
class SkillClass(
	@BitField(ordering = 0)
	val key: String,

	@BitField(ordering = 1)
	val name: String,

	@BitField(ordering = 2)
	val description: String,

	@BitField(ordering = 3)
	@CollectionField
	val actions: ArrayList<ActiveSkill>,
) {

	override fun toString() = name
}
