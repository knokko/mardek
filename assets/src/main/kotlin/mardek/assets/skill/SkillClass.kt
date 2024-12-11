package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = false)
class SkillClass(
	@BitField(ordering = 0)
	val key: String,

	@BitField(ordering = 1)
	val name: String,

	@BitField(ordering = 2)
	val description: String,

	@BitField(ordering = 3)
	val actions: ArrayList<ActiveSkill>,
) {

	internal constructor() : this("", "", "", ArrayList(0))

	override fun toString() = name
}
