package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.assets.sprite.KimSprite

@BitStruct(backwardCompatible = false)
class SkillClass(
	@BitField(ordering = 0)
	val key: String,

	@BitField(ordering = 1)
	val name: String,

	@BitField(ordering = 2)
	val description: String,

	@BitField(ordering = 3)
	@ReferenceFieldTarget(label = "skills")
	val actions: ArrayList<ActiveSkill>,

	@BitField(ordering = 4)
	val icon: KimSprite,
) {

	constructor() : this("", "", "", ArrayList(0), KimSprite())

	override fun toString() = name
}
