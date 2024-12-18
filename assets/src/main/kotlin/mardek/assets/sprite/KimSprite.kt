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

	@BitField(ordering = 2)
	@IntegerField(expectUniform = true)
	val header = if (data == null) 0 else data!![0]

	val width: Int
		get() = Kim1Decompressor.width(header)

	val height: Int
		get() = Kim1Decompressor.height(header)

	internal constructor() : this(null)
}
