package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.bitser.field.ReferenceField
import com.github.knokko.bitser.field.StableReferenceFieldId
import mardek.assets.combat.Element
import java.util.*

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

	@BitField(ordering = 7)
	@IntegerField(expectUniform = false, minValue = -1)
	var spriteIndex = -1

	// TODO Save conditionally
	@BitField(ordering = 8)
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	var sprite: IntArray? = null

	@BitField(ordering = 9)
	@StableReferenceFieldId
	val id = UUID.randomUUID()!!

	@Suppress("unused")
	private constructor() : this(
			"", "", ItemType(), null,
			0, null, null
	)

	override fun toString() = flashName
}
