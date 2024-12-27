package mardek.renderer.batch

import mardek.assets.sprite.KimSprite

class KimRequest(val x: Int, val y: Int, val scale: Float, val sprite: KimSprite, val opacity: Float) {

	override fun toString() = "KimRequest(x=$x, y=$y, scale=$scale, offset=${sprite.offset}, opacity=$opacity)"
}

class KimBatch {
	val requests = ArrayList<KimRequest>()
}
