package mardek.content.sprite

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = true)
class ObjectSprites(

	@BitField(id = 0)
	var flashName: String,

	/**
	 * The frame index into the flash spritesheet from which it was imported.
	 */
	@BitField(id = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val frameIndex: Int,

	/**
	 * In pixels
	 */
	@BitField(id = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val offsetY: Int,

	/**
	 * The number of frames that were imported from the flash spritesheet, or null if all were imported
	 */
	@BitField(id = 3, optional = true)
	@IntegerField(expectUniform = false, minValue = 1)
	val numFrames: Int?,

	@BitField(id = 4)
	val frames: Array<KimSprite>,
) {

	internal constructor() : this("", 0, 0, null, emptyArray())
}
