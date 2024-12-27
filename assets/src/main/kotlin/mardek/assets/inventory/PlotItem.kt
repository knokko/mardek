package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.combat.Element
import mardek.assets.sprite.KimSprite
import java.util.*

@BitStruct(backwardCompatible = false)
class PlotItem(
	@BitField(ordering = 0)
	val name: String,

	@BitField(ordering = 1)
	val description: String,

	@BitField(ordering = 2, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	@BitField(ordering = 3, optional = true)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int?,
) {

	@BitField(ordering = 4)
	lateinit var sprite: KimSprite

	@BitField(ordering = 5)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this("", "", null, null)
}
