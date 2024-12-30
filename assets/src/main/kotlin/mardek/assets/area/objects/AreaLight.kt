package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class AreaLight(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = true)
	val color: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false)
	val offsetY: Int
) {

	@Suppress("unused")
	private constructor() : this(0, 0)
}
