package mardek.assets.area.sprites

class ObjectSpritesheet(
	val flashName: String,
	/**
	 * In pixels
	 */
	val offsetY: Int,
	/**
	 * In pixels
	 */
	val height: Int
) {

	var frames: Array<KimImage>? = null
	var indices: IntArray? = null
}
