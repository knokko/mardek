package mardek.assets.area.sprites

import com.github.knokko.bitser.BitStruct
import com.github.knokko.bitser.field.BitField
import com.github.knokko.bitser.field.CollectionField
import com.github.knokko.bitser.field.IntegerField

@BitStruct(backwardCompatible = false)
class ObjectSpritesheet(

	@BitField(ordering = 0)
	val flashName: String,

	/**
	 * The frame index into the flash spritesheet from which it was imported.
	 */
	@BitField(ordering = 1)
	@IntegerField(expectUniform = false, minValue = 0)
	val frameIndex: Int,

	/**
	 * In pixels
	 */
	@BitField(ordering = 2)
	@IntegerField(expectUniform = false, minValue = 0)
	val offsetY: Int,

	/**
	 * The number of frames that were imported from the flash spritesheet, or null if all were imported
	 */
	@BitField(ordering = 3, optional = true)
	@IntegerField(expectUniform = false, minValue = 1)
	val numFrames: Int?,
) {

	// TODO Save this conditionally
//	@BitField(ordering = 5, optional = true)
//	@CollectionField(writeAsBytes = true)
	var frames: Array<KimImage>? = null

	@BitField(ordering = 4, optional = true)
	@CollectionField(writeAsBytes = true)
	var indices: IntArray? = null

	@Suppress("unused")
	private constructor() : this("", 0, 0, null)
}
