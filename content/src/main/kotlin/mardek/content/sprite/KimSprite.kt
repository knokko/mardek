package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting
import com.github.knokko.compressor.Kim1Decompressor
import com.github.knokko.compressor.Kim2Decompressor
import com.github.knokko.compressor.Kim3Compressor

/**
 * Represents a compressed sprite that uses the kim1 or kim3 image format (see github.com/knokko/vk-compressor).
 * These are typically small 'pixelated' images that often have a small number of distinct colors.
 */
@BitStruct(backwardCompatible = true)
class KimSprite(

	/**
	 * The raw (compressed) data of the sprite. Note that this will be `null` while in-game, but non-null while editing
	 * or importing.
	 */
	@BitField(id = 0, readsMethodResult = true)
	var data: IntArray?,

	/**
	 * The kim version of this sprite (1 for kim1, 3 for kim3).
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = true, minValue = 1, maxValue = 3, commonValues = [3])
	val version: Int,
) {

	/**
	 * The index of this sprite into the kim sprite/fake image list of the renderer. The value of this is only
	 * meaningful while in-game; it should be -1 during editing and importing. This variable should get the right
	 * value during exporting.
	 */
	@BitField(id = 2)
	@IntegerField(minValue = -1, expectUniform = false, digitSize = 3)
	var index = -1

	/**
	 * The header of this sprite, which is simply `data[0]`. This contains the width and height of the image, which is
	 * the only part of [data] that is needed in-game.
	 */
	@BitField(id = 3)
	@IntegerField(expectUniform = true, commonValues=[ // Don't question it: it just saves storage space
		83951632, 100728848, 50397200, 67174416, 117506064, 151060496, 16842768, 134283280,
		167837712, 184614928, 33619984, 201392144, 218169360, 234946576, 251723792
	])
	val header = if (data == null) 0 else data!![0]

	/**
	 * The width of this sprite, in pixels
	 */
	val width: Int
		get() = when (version) {
			1 -> Kim1Decompressor.width(header)
			2 -> Kim2Decompressor.getWidth(header)
			else -> Kim3Compressor.getWidth(header)
		}

	/**
	 * The height of this sprite, in pixels
	 */
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

	@BitField(id = 0)
	@Suppress("unused")
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	private fun saveData(context: FunctionContext): IntArray? {
		return if (context.withParameters.containsKey("exporting")) null
		else data
	}
}
