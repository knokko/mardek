package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.combat.Element

@BitStruct(backwardCompatible = false)
class Item(
	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 1)
	val description: String,

	@BitField(ordering = 2)
	@ReferenceField(stable = false, label = "item types")
	val type: ItemType,

	@BitField(ordering = 3, optional = true)
	@ReferenceField(stable = false, label = "elements")
	val element: Element?,

	@BitField(ordering = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int,

	@BitField(ordering = 5, optional = true)
	val equipment: EquipmentProperties?,

	@BitField(ordering = 6, optional = true)
	val consumable: ConsumableProperties?,
) {

	// TODO sprite

	@Suppress("unused")
	private constructor() : this(
			"", "", ItemType(), null,
			0, null, null
	)

	override fun toString() = flashName
}
