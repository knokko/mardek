package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.content.stats.Element
import mardek.content.sprite.KimSprite
import java.util.*

@BitStruct(backwardCompatible = true)
class PlotItem(
	@BitField(id = 0)
	val name: String,

	@BitField(id = 1)
	val description: String,

	@BitField(id = 2, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	@BitField(id = 3, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int?,
) {

	@BitField(id = 4)
	lateinit var sprite: KimSprite

	@BitField(id = 5)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	constructor() : this("", "", null, null)

	override fun toString() = name
}
