package mardek.assets.skill

import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element

abstract class Skill(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val description: String,

	@BitField(ordering = 2)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = -1)
	val masteryPoints: Int,
) {

	override fun toString() = name
}
