package mardek.assets.area.sprites

import com.github.knokko.compressor.Kim1Decompressor

@JvmInline
value class KimImage(val data: IntArray) {

	val width: Int
		get() = Kim1Decompressor.width(data[0])

	val height: Int
		get() = Kim1Decompressor.height(data[0])
}
