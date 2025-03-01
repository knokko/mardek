package mardek.assets.animations

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class ColorTransform(

	@BitField(id = 0)
	@IntegerField(expectUniform = true)
	val addColor: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = true)
	val multiplyColor: Int,
) {

	constructor() : this(0, 0)
}
