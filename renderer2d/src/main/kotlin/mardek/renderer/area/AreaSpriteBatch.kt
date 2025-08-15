package mardek.renderer.area

import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import mardek.content.sprite.KimSprite
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

class AreaSpriteBatch(
	pipeline: AreaSpritePipeline, frame: Vk2dRenderStage,
	initialCapacity: Int,
	internal val bundle: Vk2dResourceBundle,
	internal val perFrameDescriptorSet: Long,
	internal val scissor: Rectangle,
): Vk2dBatch(pipeline, frame, initialCapacity) {

	fun draw(sprite: KimSprite, x: Int, y: Int, scale: Float, blinkColor: Int = 0, opacity: Float = 1f) {
		val quad = putTriangles(2).vertexData[0]
		val boundX = x + (scale * sprite.width).roundToInt()
		val boundY = y + (scale * sprite.height).roundToInt()

		val textureIndex = bundle.getFakeImageOffset(sprite.index)
		putCompressedPosition(quad, x, y)
		putCompressedPosition(quad, boundX - x, boundY - y)
		quad.putInt(textureIndex)
		quad.putInt(blinkColor)
		quad.putFloat(opacity)
	}

	fun draw(sprite: KimSprite, x: Int, y: Int, scale: Int, blinkColor: Int = 0, opacity: Float = 1f) {
		draw(sprite, x, y, scale.toFloat(), blinkColor, opacity)
	}
}
