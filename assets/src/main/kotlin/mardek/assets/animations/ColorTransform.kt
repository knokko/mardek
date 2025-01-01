package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class ColorTransform(

	@BitField(ordering = 0)
	@IntegerField(expectUniform = true)
	val addColor: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = true)
	val multiplyColor: Int,
) {

	constructor() : this(0, 0)
}
