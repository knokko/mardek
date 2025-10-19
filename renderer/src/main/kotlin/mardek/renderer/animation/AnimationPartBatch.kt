package mardek.renderer.animation

import com.github.knokko.vk2d.batch.Vk2dBatch
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.resource.Vk2dResourceBundle

class AnimationPartBatch(
	pipeline: AnimationPartPipeline,
	stage: Vk2dRenderStage,
	capacity: Int,
	private val bundle: Vk2dResourceBundle,
) : Vk2dBatch(pipeline, stage, capacity) {

	internal var mainDescriptorSets = LongArray(capacity)
	internal var maskDescriptorSets = LongArray(capacity)
	private var nextDescriptorIndex = 0

	fun transformed(
		x1: Float, y1: Float, x2: Float, y2: Float,
		x3: Float, y3: Float, x4: Float, y4: Float,
		maskTx1: Float, maskTy1: Float, maskTx2: Float, maskTy2: Float,
		maskTx3: Float, maskTy3: Float, maskTx4: Float, maskTy4: Float,
		mainImageIndex: Int, maskImageIndex: Int,
		addColor: Int, multiplyColor: Int,
	) {
		val vertices = putTriangles(2).vertexData[0]

		vertices.putFloat(x1).putFloat(y1)
		vertices.putFloat(0f).putFloat(1f)
		vertices.putFloat(maskTx1).putFloat(maskTy1)
		vertices.putInt(addColor).putInt(multiplyColor)
		vertices.putFloat(x2).putFloat(y2)
		vertices.putFloat(1f).putFloat(1f)
		vertices.putFloat(maskTx2).putFloat(maskTy2)
		vertices.putInt(addColor).putInt(multiplyColor)
		vertices.putFloat(x3).putFloat(y3)
		vertices.putFloat(1f).putFloat(0f)
		vertices.putFloat(maskTx3).putFloat(maskTy3)
		vertices.putInt(addColor).putInt(multiplyColor)

		vertices.putFloat(x3).putFloat(y3)
		vertices.putFloat(1f).putFloat(0f)
		vertices.putFloat(maskTx3).putFloat(maskTy3)
		vertices.putInt(addColor).putInt(multiplyColor)
		vertices.putFloat(x4).putFloat(y4)
		vertices.putFloat(0f).putFloat(0f)
		vertices.putFloat(maskTx4).putFloat(maskTy4)
		vertices.putInt(addColor).putInt(multiplyColor)
		vertices.putFloat(x1).putFloat(y1)
		vertices.putFloat(0f).putFloat(1f)
		vertices.putFloat(maskTx1).putFloat(maskTy1)
		vertices.putInt(addColor).putInt(multiplyColor)

		if (nextDescriptorIndex >= mainDescriptorSets.size) {
			mainDescriptorSets = mainDescriptorSets.copyOf(2 * mainDescriptorSets.size)
			maskDescriptorSets = maskDescriptorSets.copyOf(2 * maskDescriptorSets.size)
		}
		mainDescriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(mainImageIndex)
		maskDescriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(maskImageIndex)
		nextDescriptorIndex += 1
	}
}
