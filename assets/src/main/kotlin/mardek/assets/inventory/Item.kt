package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.ReferenceField

@BitStruct(backwardCompatible = false)
class Item(
	@BitField(ordering = 0, optional = true)
	val flashName: String?,

	@BitField(ordering = 1)
	@ReferenceField(stable = false, label = "item types")
	val type: ItemType,

	@BitField(ordering = 2, optional = true)
	val weapon: WeaponProperties?,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val cost: Int,

	// TODO elem, only_user, effects, skills, desc
) {

}
