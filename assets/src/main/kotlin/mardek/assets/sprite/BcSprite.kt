package mardek.assets.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

@BitStruct(backwardCompatible = false)
class BcSprite(
	@BitField(ordering = 0)
	@IntegerField(expectUniform = false, minValue = 1)
	val width: Int,

	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 1)
	val height: Int,

	@BitField(ordering = 2)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 7)
	val version: Int,
) {
	var bufferedImage: Any? = null

	var postEncodeCallback: (() -> Unit)? = null

	@BitField(ordering = 3)
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	var data: ByteArray? = null

	@BitField(ordering = 4)
	@IntegerField(minValue = -1, expectUniform = true)
	var index = -1

	constructor() : this(0, 0, 0)
}
