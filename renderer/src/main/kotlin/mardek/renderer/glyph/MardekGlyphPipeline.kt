package mardek.renderer.glyph

import com.github.knokko.boiler.commands.CommandRecorder
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder
import com.github.knokko.vk2d.frame.Vk2dRenderStage
import com.github.knokko.vk2d.Vk2dInstance
import com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline
import com.github.knokko.vk2d.pipeline.Vk2dPipelineContext
import com.github.knokko.vk2d.text.Vk2dTextBuffer

class MardekGlyphPipeline(context: Vk2dPipelineContext, instance: Vk2dInstance): Vk2dGlyphPipeline(context, instance) {

	override fun getBytesPerTriangle() = BYTES_PER_TRIANGLE

	override fun getVertexAlignments() = VERTEX_ALIGNMENTS

	override fun setShaderStages(builder: GraphicsPipelineBuilder) {
		builder.simpleShaderStages(
			"MardekGlyph", "mardek/renderer/glyph/",
			"fancy.vert.spv", "fancy.frag.spv"
		)
	}

	override fun addBatch(
		stage: Vk2dRenderStage, initialCapacity: Int, recorder: CommandRecorder,
		textBuffer: Vk2dTextBuffer, perFrameDescriptorSet: Long
	): MardekGlyphBatch {
		return MardekGlyphBatch(
			this, stage, initialCapacity, textBuffer, perFrameDescriptorSet
		)
	}

	companion object {
		private const val GLYPH_SIZE = 8 * 16
		private val BYTES_PER_TRIANGLE = intArrayOf(3 * VERTEX_SIZE, GLYPH_SIZE / 2)
		private val VERTEX_ALIGNMENTS = intArrayOf(6 * VERTEX_SIZE, GLYPH_SIZE)
	}
}
