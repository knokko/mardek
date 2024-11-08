package mardek.assets.area.sprites

class ObjectSpritesheet(
	val flashName: String,
	/**
	 * The frame index into the flash spritesheet from which it was imported.
	 */
	val frameIndex: Int,
	/**
	 * In pixels
	 */
	val offsetY: Int,
	/**
	 * The number of frames that were imported from the flash spritesheet, or null if all were imported
	 */
	val numFrames: Int?,
) {

	var frames: Array<KimImage>? = null
	var indices: IntArray? = null
}
