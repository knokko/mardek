package mardek.renderer.batch

import mardek.content.sprite.KimSprite

class KimRequest(
	val x: Int, val y: Int, val scale: Float, val sprite: KimSprite,
	val opacity: Float = 1f, val rotation: Float = 0f,
) {

	override fun toString() = "KimRequest(x=$x, y=$y, scale=$scale, offset=${sprite.offset}, opacity=$opacity, rotation=$rotation)"
}

class KimBatch {
	val requests = ArrayList<KimRequest>()
}
