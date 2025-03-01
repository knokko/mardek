package mardek.assets.skill

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget
import mardek.assets.sprite.KimSprite

@BitStruct(backwardCompatible = true)
class SkillClass(
	@BitField(id = 0)
	val key: String,

	@BitField(id = 1)
	val name: String,

	@BitField(id = 2)
	val description: String,

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "skills")
	val actions: ArrayList<ActiveSkill>,

	@BitField(id = 4)
	val icon: KimSprite,
) {

	constructor() : this("", "", "", ArrayList(0), KimSprite())

	override fun toString() = name
}
