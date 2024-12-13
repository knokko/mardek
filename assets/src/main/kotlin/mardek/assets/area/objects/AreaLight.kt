package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class AreaLight(

	@BitField(ordering = 0)
	val colorName: String,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false)
	val offsetY: Int
) {

	@Suppress("unused")
	private constructor() : this("", 0)
}
