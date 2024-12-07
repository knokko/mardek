package mardek.assets.inventory

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.StringField

@BitStruct(backwardCompatible = false)
class ItemType(
	@BitField(ordering = 0)
	val flashName: String,

	@BitField(ordering = 1)
	val canStack: Boolean,

	@BitField(ordering = 2)
	@StringField(length = IntegerField(expectUniform = true, minValue = 1, maxValue = 1))
	val sheetChar: String,

	@BitField(ordering = 3)
	@IntegerField(expectUniform = false, minValue = 0)
	val sheetNumber: Int,
) {

	@Suppress("unused")
	private constructor() : this("", false, " ", 0)
}
