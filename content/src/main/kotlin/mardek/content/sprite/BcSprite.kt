package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.FunctionContext
import com.github.knokko.bitser.field.IntegerField
import com.github.knokko.bitser.field.NestedFieldSetting

/**
 * Represents a sprite that is usually compressed using a BC texture format. This is a lossy compression format that
 * takes 1 byte per pixel (BC7) or 0.5 bytes per pixel (BC1 and BC4). This is a big improvement compared to standard
 * RGBA encoding, which takes 4 bytes per pixel.
 */
@BitStruct(backwardCompatible = true)
class BcSprite(

	/**
	 * The width of the sprite, in pixels
	 */
	@BitField(id = 0)
	@IntegerField(expectUniform = false, minValue = 1, digitSize = 2)
	val width: Int,

	/**
	 * The height of the sprite, in pixels
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 1, digitSize = 2)
	val height: Int,

	/**
	 * The BC encoding version of the sprite (1 for BC1, 7 for BC7, etc...), or 0 if this sprite is *not* compressed.
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 7, commonValues = [7])
	val version: Int,

	/**
	 * This field determines what happens around the edges of the image:
	 * - When `false`, the 'pixels' beyond the image are assumed to be `rgba(0, 0, 0, 0)`. This is the most common
	 * option.
	 * - When `true`, the 'pixels' beyond the image are assumed to have the same color as the nearest pixel
	 * that is *inside* the image. This is a rather niche option that we use for mask sprites.
	 */
	@BitField(id = 3)
	val clamped: Boolean,

	/**
	 * Sometimes, we increase the line width of the sprites we import, to make them look better.
	 * However, when a line is too close to the border of the image,
	 * the 'thickened' part of the line would be outside the image, and thus invisible.
	 *
	 * To work around this, we first make the image bigger by adding transparent space around all borders.
	 * The `artificialOffset` is the width of this transparent space,
	 * and is needed to render the image at the correct position.
	 */
	@BitField(id = 4)
	@IntegerField(expectUniform = false, minValue = 0)
	val artificialOffset: Int,
) {
	var bufferedImage: Any? = null

	/**
	 * The raw (compressed) data of the sprite. Note that this will be `null` while in-game, but non-null while editing
	 * or importing.
	 */
	@BitField(id = 5, readsMethodResult = true)
	var data: ByteArray? = null

	/**
	 * The index of this sprite into the `VkImage` list of the renderer. The value of this is only meaningful while
	 * in-game; it should be -1 during editing and importing. This variable should get the right value during
	 * exporting.
	 */
	@BitField(id = 6)
	@IntegerField(minValue = -1, expectUniform = false, digitSize = 3)
	var index = -1

	constructor() : this(0, 0, 0, false, 0)

	@BitField(id = 5)
	@Suppress("unused")
	@NestedFieldSetting(path = "", optional = true, writeAsBytes = true)
	private fun saveData(context: FunctionContext): ByteArray? {
		return if (context.withParameters.containsKey("exporting")) null else data
	}

	/**
	 * Creates a shallow copy of this sprite, with an independent [index].
	 *
	 * Note that the [bufferedImage] and [data] are *not* copied: they will be shared!
	 */
	fun shallowCopy(): BcSprite {
		val copied = BcSprite(width, height, version, clamped, artificialOffset)
		copied.bufferedImage = this.bufferedImage
		copied.data = this.data
		copied.index = this.index
		return copied
	}
}
