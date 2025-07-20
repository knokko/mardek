package mardek.content.ui

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField

@BitStruct(backwardCompatible = true)
class Fonts(
	@BitField(id = 0)
	val basic: Font,

	@BitField(id = 1)
	val basicLarge: Font,
) {

	@Suppress("unused")
	private constructor() : this(Font(), Font())
}
