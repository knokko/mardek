package mardek.renderer.animation

import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.resource.Vk2dResourceBundle
import mardek.state.util.Rectangle
import org.joml.Matrix3x2f
import org.joml.Vector2f

class AnimationPartBatch(
	pipeline: AnimationPartPipeline,
	stage: Vk2dRenderStage,
	capacity: Int,
	private val bundle: Vk2dResourceBundle,
) : Vk2dBatch(pipeline, stage, capacity) {

	internal var mainDescriptorSets = LongArray(capacity)
	internal var maskDescriptorSets = LongArray(capacity)
	private var nextDescriptorIndex = 0

	@Suppress("unused")
	fun transformed(
		x1: Float, y1: Float, x2: Float, y2: Float,
		x3: Float, y3: Float, x4: Float, y4: Float,
		maskX1: Float, maskY1: Float, maskX2: Float, maskY2: Float,
		maskX3: Float, maskY3: Float, maskX4: Float, maskY4: Float,
		mainImageIndex: Int, maskImageIndex: Int, region: Rectangle,
		addColor: Int, multiplyColor: Int, subtractColor: Int,
	) {
		// Texture coordinate math:
		// posX = ax * texX + bx * texY + cx
		// posY = ay * texX + by * texY + cy
		//
		// (0, 0) gets transformed to (x4, y4) so cx = x4 and cy = y4
		// (0, 1) gets transformed to (x1, y1) which implies:
		//   ax * 0 + bx * 1 + cx = bx + x4 = x1 -> bx = x1 - x4
		//   ay * 0 + by * 1 + cy = by + y4 = y1 -> by = y1 - y4
		// (1, 1) gets transformed to (x2, y2) which implies:
		//   ax * 1 + bx * 1 + cx = ax + (x1 - x4) + x4 = ax + x1 = x2 -> ax = x2 - x1
		//   ay * 1 + by * 1 + cy = ay + (y1 - y4) + y4 = ay + y1 = y2 -> ay = y2 - y1
		val texToPos = Matrix3x2f(
			x2 - x1, y2 - y1,
			x1 - x4, y1 - y4,
			x4, y4,
		)
		val posToTex = texToPos.invert()

		// Same holds for mask texture coordinates
		val maskTexToPos = Matrix3x2f(
			maskX2 - maskX1, maskY2 - maskY1,
			maskX1 - maskX4, maskY1 - maskY4,
			maskX4, maskY4,
		)
		val posToMaskTex = maskTexToPos.invert(Matrix3x2f())
		if (!posToTex.isFinite || !posToMaskTex.isFinite) return

		val minX = 2f * region.minX / width - 1f
		val maxX = 2f * region.boundX / width - 1
		val minY = 2f * region.minY / height - 1f
		val maxY = 2f * region.boundY / height - 1f
		val vertices = putTriangles(2).vertexData[0]

		val clippedX1 = x1.coerceIn(minX, maxX)
		val clippedX2 = x2.coerceIn(minX, maxX)
		val clippedX3 = x3.coerceIn(minX, maxX)
		val clippedX4 = x4.coerceIn(minX, maxX)

		val clippedY1 = y1.coerceIn(minY, maxY)
		val clippedY2 = y2.coerceIn(minY, maxY)
		val clippedY3 = y3.coerceIn(minY, maxY)
		val clippedY4 = y4.coerceIn(minY, maxY)

		val tex1 = posToTex.transformPosition(clippedX1, clippedY1, Vector2f())
		val tex2 = posToTex.transformPosition(clippedX2, clippedY2, Vector2f())
		val tex3 = posToTex.transformPosition(clippedX3, clippedY3, Vector2f())
		val tex4 = posToTex.transformPosition(clippedX4, clippedY4, Vector2f())

		val maskTex1 = posToMaskTex.transformPosition(clippedX1, clippedY1, Vector2f())
		val maskTex2 = posToMaskTex.transformPosition(clippedX2, clippedY2, Vector2f())
		val maskTex3 = posToMaskTex.transformPosition(clippedX3, clippedY3, Vector2f())
		val maskTex4 = posToMaskTex.transformPosition(clippedX4, clippedY4, Vector2f())

		vertices.putFloat(clippedX1).putFloat(clippedY1)
		vertices.putFloat(tex1.x).putFloat(tex1.y)
		vertices.putFloat(maskTex1.x).putFloat(maskTex1.y)
		vertices.putInt(addColor).putInt(multiplyColor).putInt(subtractColor)
		vertices.putFloat(clippedX2).putFloat(clippedY2)
		vertices.putFloat(tex2.x).putFloat(tex2.y)
		vertices.putFloat(maskTex2.x).putFloat(maskTex2.y)
		vertices.putInt(addColor).putInt(multiplyColor).putInt(subtractColor)
		vertices.putFloat(clippedX3).putFloat(clippedY3)
		vertices.putFloat(tex3.x).putFloat(tex3.y)
		vertices.putFloat(maskTex3.x).putFloat(maskTex3.y)
		vertices.putInt(addColor).putInt(multiplyColor).putInt(subtractColor)

		vertices.putFloat(clippedX3).putFloat(clippedY3)
		vertices.putFloat(tex3.x).putFloat(tex3.y)
		vertices.putFloat(maskTex3.x).putFloat(maskTex3.y)
		vertices.putInt(addColor).putInt(multiplyColor).putInt(subtractColor)
		vertices.putFloat(clippedX4).putFloat(clippedY4)
		vertices.putFloat(tex4.x).putFloat(tex4.y)
		vertices.putFloat(maskTex4.x).putFloat(maskTex4.y)
		vertices.putInt(addColor).putInt(multiplyColor).putInt(subtractColor)
		vertices.putFloat(clippedX1).putFloat(clippedY1)
		vertices.putFloat(tex1.x).putFloat(tex1.y)
		vertices.putFloat(maskTex1.x).putFloat(maskTex1.y)
		vertices.putInt(addColor).putInt(multiplyColor).putInt(subtractColor)

		if (nextDescriptorIndex >= mainDescriptorSets.size) {
			mainDescriptorSets = mainDescriptorSets.copyOf(2 * mainDescriptorSets.size)
			maskDescriptorSets = maskDescriptorSets.copyOf(2 * maskDescriptorSets.size)
		}
		mainDescriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(mainImageIndex)
		maskDescriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(maskImageIndex)
		nextDescriptorIndex += 1
	}
}
