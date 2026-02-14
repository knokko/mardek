package mardek.renderer.area.water

import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import mardek.content.sprite.KimSprite
import mardek.state.util.Rectangle

class SimpleWaterBatch(
	pipeline: SimpleWaterPipeline, frame: Vk2dRenderStage,
	initialCapacity: Int,
	internal val bundle: Vk2dResourceBundle,
	internal val perFrameDescriptorSet: Long,
	internal val scissor: Rectangle,
	internal val scale: Int,
): Vk2dBatch(pipeline, frame, initialCapacity) {

	fun draw(
		backgroundSprite: KimSprite, waterSprite: KimSprite,
		cornerX: Int, cornerY: Int, tileX: Int, tileY: Int,
	) {
		val quad = putTriangles(2).vertexData[0]

		val backgroundTextureIndex = bundle.getFakeImageOffset(backgroundSprite.index)
		val waterTextureIndex = bundle.getFakeImageOffset(waterSprite.index)
		putCompressedPosition(quad, tileX, tileY)
		putCompressedPosition(quad, cornerX, cornerY)
		quad.putInt(backgroundTextureIndex)
		quad.putInt(waterTextureIndex)
	}
}
