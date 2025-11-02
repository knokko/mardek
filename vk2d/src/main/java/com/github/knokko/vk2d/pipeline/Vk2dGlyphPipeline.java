package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dGlyphPipeline extends Vk2dPipeline {

	protected static final int VERTEX_SIZE = 4;
	public static final int GLYPH_SIZE = 64;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE, GLYPH_SIZE / 2 };

	// We need an alignment of 3 * VERTEX_SIZE since the vertex shader uses gl_VertexIndex % 6 for calculations
	private static final int[] VERTEX_ALIGNMENTS = { 3 * VERTEX_SIZE, GLYPH_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dGlyphPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super(context.printBatchSizes);

		this.vkPipelineLayout = instance.textIntersectionPipelineLayout;
		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(1, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32_UINT, 0);

			var builder = pipelineBuilder(context, stack);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			setShaderStages(builder);
			builder.ciPipeline.layout(instance.textIntersectionPipelineLayout);

			this.vkPipeline = builder.build("Vk2dGlyphPipeline");
		}
	}

	protected void setShaderStages(GraphicsPipelineBuilder builder) {
		builder.simpleShaderStages(
				"Glyph", "com/github/knokko/vk2d/glyph/",
				"basic.vert.spv", "basic.frag.spv"
		);
	}

	public Vk2dGlyphBatch addBatch(
			Vk2dRenderStage frame, int initialCapacity, CommandRecorder recorder,
			Vk2dTextBuffer textBuffer, long perFrameDescriptorSet
	) {
		return new Vk2dGlyphBatch(
				this, frame, initialCapacity, textBuffer, perFrameDescriptorSet
		);
	}

	@Override
	protected int[] getBytesPerTriangle() {
		return BYTES_PER_TRIANGLE;
	}

	@Override
	protected int[] getVertexAlignments() {
		return VERTEX_ALIGNMENTS;
	}

	@Override
	public void prepareRecording(CommandRecorder recorder, Vk2dBatch batch) {
		super.prepareRecording(recorder, batch);

		Vk2dGlyphBatch glyphBatch = (Vk2dGlyphBatch) batch;
		recorder.bindGraphicsDescriptors(
				vkPipelineLayout, glyphBatch.textBuffer.getRenderDescriptorSet(),
				glyphBatch.perFrameDescriptorSet
		);
		vkCmdPushConstants(
				recorder.commandBuffer, vkPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
				0, recorder.stack.ints(batch.width, batch.height)
		);
	}

	@Override
	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MiniBatch miniBatch, Vk2dBatch batch) {
		recordNonIndexedBatch(recorder, perFrameBuffer, miniBatch);
	}
}
