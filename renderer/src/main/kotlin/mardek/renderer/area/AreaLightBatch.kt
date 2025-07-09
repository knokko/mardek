package mardek.renderer.area

import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.batch.Vk2dBatch
import mardek.state.util.Rectangle

class AreaLightBatch(
	pipeline: AreaLightPipeline, frame: Vk2dRenderStage,
	internal val perFrameDescriptorSet: Long,
	internal val scissor: Rectangle,
): Vk2dBatch(pipeline, frame, 50) {

	fun draw(minX: Int, minY: Int, radius: Int, color: Int) {
		val quad = putTriangles(2).vertexData[0]
		putCompressedPosition(quad, minX, minY)
		quad.putInt(radius)
		quad.putInt(color)
	}
}
