package mardek.assets.skill

import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.combat.Element
import java.util.*

abstract class Skill(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val description: String,

	@BitField(id = 2)
	@ReferenceField(stable = false, label = "elements")
	val element: Element,

	@BitField(id = 3)
	@IntegerField(expectUniform = false, minValue = -1)
	val masteryPoints: Int,
) {

	@BitField(id = 4)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	override fun toString() = name
}
