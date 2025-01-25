package mardek.assets.battle

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField
import mardek.assets.inventory.Item

@BitStruct(backwardCompatible = false)
class PotentialItem(

	@BitField(ordering = 0)
	@ReferenceField(stable = false, label = "items")
	val item: Item,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 100)
	val chance: Int,
) {

	constructor() : this(Item(), 100)
}

@BitStruct(backwardCompatible = false)
class PotentialEquipment(

	@BitField(ordering = 0)
	val entries: ArrayList<PotentialItem>
) {

	constructor() : this(arrayListOf(PotentialItem()))
}
