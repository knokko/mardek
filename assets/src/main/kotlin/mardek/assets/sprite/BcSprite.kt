package mardek.assets.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

@BitStruct(backwardCompatible = true)
class BcSprite(
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,

	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val height: Int,

	@BitField(id = 2)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 7)
	val version: Int,
) {
	var bufferedImage: Any? = null

	var postEncodeCallback: (() -> Unit)? = null

	@BitField(id = 3)
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	var data: ByteArray? = null

	@BitField(id = 4)
	@IntegerField(minValue = -1, expectUniform = true)
	var index = -1

	constructor() : this(0, 0, 0)
}
