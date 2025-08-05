package mardek.renderer.area

import com.github.knokko.vk2d.Vk2dFrame
import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import mardek.content.sprite.KimSprite
import mardek.state.util.Rectangle
import kotlin.math.roundToInt

class AreaSpriteBatch(
	pipeline: AreaSpritePipeline, frame: Vk2dFrame,
	internal val bundle: Vk2dResourceBundle,
	internal val scissor: Rectangle,
): Vk2dBatch(pipeline, frame, 1000) {

	fun draw(sprite: KimSprite, x: Int, y: Int, scale: Float, blinkColor: Int = 0, opacity: Float = 1f) {
		val vertices = putTriangles(2).vertexData[0]
		val boundX = x + (scale * sprite.width).roundToInt()
		val boundY = y + (scale * sprite.height).roundToInt()

		val textureIndex = bundle.getFakeImageOffset(sprite.offset)
		fun putData() {
			putCompressedPosition(vertices, boundX - x, boundY - y)
			vertices.putInt(textureIndex)
			vertices.putInt(blinkColor)
			vertices.putFloat(opacity)
		}


		putCompressedPosition(vertices, x, boundY)
		putData()
		putCompressedPosition(vertices, boundX, boundY)
		putData()
		putCompressedPosition(vertices, boundX, y)
		putData()

		putCompressedPosition(vertices, boundX, y)
		putData()
		putCompressedPosition(vertices, x, y)
		putData()
		putCompressedPosition(vertices, x, boundY)
		putData()
	}

	fun draw(sprite: KimSprite, x: Int, y: Int, scale: Int, blinkColor: Int = 0, opacity: Float = 1f) {
		draw(sprite, x, y, scale.toFloat(), blinkColor, opacity)
	}
}
