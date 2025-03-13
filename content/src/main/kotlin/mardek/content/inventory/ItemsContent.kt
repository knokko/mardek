package mardek.content.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = true)
class ItemsContent(
	@BitField(id = 0)
	@ReferenceFieldTarget(label = "item types")
	val itemTypes: ArrayList<ItemType>,

	@BitField(id = 1)
	@ReferenceFieldTarget(label = "weapon types")
	val weaponTypes: ArrayList<WeaponType>,

	@BitField(id = 2)
	@ReferenceFieldTarget(label = "armor types")
	val armorTypes: ArrayList<ArmorType>,

	@BitField(id = 3)
	@ReferenceFieldTarget(label = "items")
	val items: ArrayList<Item>,

	@BitField(id = 4)
	@ReferenceFieldTarget(label = "plot items")
	val plotItems: ArrayList<PlotItem>,

	@BitField(id = 5)
	@ReferenceFieldTarget(label = "dreamstones")
	val dreamstones: ArrayList<Dreamstone>,
) {
	constructor() : this(
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0)
	)
}
