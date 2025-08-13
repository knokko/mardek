package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.compressor.Kim1Decompressor
import com.github.knokko.compressor.Kim2Decompressor
import com.github.knokko.vk2d.Kim3Compressor

// TODO Save conditionally
@BitStruct(backwardCompatible = true)
class KimSprite(
	@BitField(id = 0)
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	var data: IntArray?,

	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 3)
	val version: Int,
) {

	@BitField(id = 2)
	@IntegerField(minValue = -1, expectUniform = true)
	var index = -1

	@BitField(id = 3)
	@IntegerField(expectUniform = true)
	val header = if (data == null) 0 else data!![0]

	val width: Int
		get() = when (version) {
			1 -> Kim1Decompressor.width(header)
			2 -> Kim2Decompressor.getWidth(header)
			else -> Kim3Compressor.getWidth(header)
		}

	val height: Int
		get() = when (version) {
			1 -> Kim1Decompressor.height(header)
			2 -> Kim2Decompressor.getWidth(header)
			else -> Kim3Compressor.getHeight(header)
		}

	init {
		if (version != 1 && version != 2 && version != 3) throw IllegalArgumentException("Unknown KIM version $version")
	}

	constructor() : this(null, 1)
}
