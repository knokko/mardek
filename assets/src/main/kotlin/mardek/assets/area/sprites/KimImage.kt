package mardek.assets.area.sprites

@JvmInline
value class KimImage(val data: IntArray) {

	// TODO width and height should probably become part of vk-compressor
	val width: Int
		get() = data[0] and 1023

	val height: Int
		get() = (data[0] shr 10) and 1023
}
