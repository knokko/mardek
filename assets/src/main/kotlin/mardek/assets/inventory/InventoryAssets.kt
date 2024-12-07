package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.ReferenceFieldTarget

@BitStruct(backwardCompatible = false)
class InventoryAssets(
		@CollectionField
		@ReferenceFieldTarget(stable = false, label = "item types")
		val itemTypes: ArrayList<ItemType>,

		@CollectionField
		@ReferenceFieldTarget(stable = false, label = "weapon types")
		val weaponTypes: ArrayList<WeaponType>,

		@CollectionField
		@ReferenceFieldTarget(stable = false, label = "armor types")
		val armorTypes: ArrayList<ArmorType>,

		@CollectionField
		@ReferenceFieldTarget(stable = false, label = "items")
		val items: ArrayList<Item>,
) {
	constructor() : this(
			ArrayList(0), ArrayList(0),
			ArrayList(0), ArrayList(0)
	)
}
