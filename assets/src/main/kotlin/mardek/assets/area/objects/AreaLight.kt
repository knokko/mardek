package mardek.assets.area.objects

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class AreaLight(

	@BitField(id = 0)
	@IntegerField(expectUniform = true)
	val color: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false)
	val offsetY: Int
) {

	@Suppress("unused")
	private constructor() : this(0, 0)
}
