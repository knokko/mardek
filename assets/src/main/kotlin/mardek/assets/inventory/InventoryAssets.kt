package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class InventoryAssets(
	@BitField(ordering = 0)
	@ReferenceFieldTarget(label = "item types")
	val itemTypes: ArrayList<ItemType>,

	@BitField(ordering = 1)
	@ReferenceFieldTarget(label = "weapon types")
	val weaponTypes: ArrayList<WeaponType>,

	@BitField(ordering = 2)
	@ReferenceFieldTarget(label = "armor types")
	val armorTypes: ArrayList<ArmorType>,

	@BitField(ordering = 3)
	@ReferenceFieldTarget(label = "items")
	val items: ArrayList<Item>,

	@BitField(ordering = 4)
	@ReferenceFieldTarget(label = "plot items")
	val plotItems: ArrayList<PlotItem>,

	@BitField(ordering = 5)
	@ReferenceFieldTarget(label = "dreamstones")
	val dreamstones: ArrayList<Dreamstone>,
) {
	constructor() : this(
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0),
		ArrayList(0), ArrayList(0)
	)
}
