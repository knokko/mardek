package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class InventoryAssets(
		@ReferenceFieldTarget(label = "item types")
		val itemTypes: ArrayList<ItemType>,

		@ReferenceFieldTarget(label = "weapon types")
		val weaponTypes: ArrayList<WeaponType>,

		@ReferenceFieldTarget(label = "armor types")
		val armorTypes: ArrayList<ArmorType>,

		@ReferenceFieldTarget(label = "items")
		val items: ArrayList<Item>,
) {
	constructor() : this(
			ArrayList(0), ArrayList(0),
			ArrayList(0), ArrayList(0)
	)
}
