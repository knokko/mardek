package mardek.assets.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.compressor.Kim1Decompressor

// TODO Save conditionally
@BitStruct(backwardCompatible = false)
class KimSprite(
	@BitField(ordering = 0)
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	var data: IntArray?
) {

	@BitField(ordering = 1)
	@IntegerField(minValue = -1, expectUniform = true)
	var offset = -1

	val width: Int
		get() = Kim1Decompressor.width(data!![0])

	val height: Int
		get() = Kim1Decompressor.height(data!![0])

	internal constructor() : this(null)
}
