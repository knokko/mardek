package mardek.renderer.area

import com.github.knokko.boiler.utilities.ColorPacker.multiplyAlpha
import mardek.content.sprite.KimSprite

internal class SpriteRenderJob(
	val x: Int, val y: Int, val sprite: KimSprite, val opacity: Float = 1f, val sortY: Int = y,
	val blinkColor: Int = 0, val blinkIntensity: Float = 0f,
): Comparable<SpriteRenderJob> {

	init {
		if (sprite.index == -1) throw IllegalArgumentException()
	}

	override fun compareTo(other: SpriteRenderJob) = this.sortY.compareTo(other.sortY)

	fun addToBatch(areaContext: AreaRenderContext) {
		areaContext.apply {
			var renderX = region.minX + x + region.width / 2 - cameraX
			var renderY = region.minY + y + region.height / 2 - cameraY
			if (sprite.width >= 32) renderX -= 16 * scale
			if (sprite.height >= 32) renderY -= (sprite.height - 16) * scale
			val margin = 2 * tileSize
			if (renderX > -margin && renderY > -margin && renderX < region.width + 2 * margin &&
				renderY < region.height + 2 * margin
			) {
				val blinkColor = multiplyAlpha(blinkColor, blinkIntensity)
				spriteBatch.draw(sprite, renderX, renderY, scale, blinkColor, opacity)
			}
		}
	}
}
