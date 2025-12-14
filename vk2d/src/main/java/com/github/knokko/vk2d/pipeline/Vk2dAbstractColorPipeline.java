package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

abstract class Vk2dAbstractColorPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 8;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE };
	private static final int[] VERTEX_ALIGNMENTS = { VERTEX_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	Vk2dAbstractColorPipeline(Vk2dPipelineContext context, long vkPipelineLayout) {
		super(context.printBatchSizes);
		this.vkPipelineLayout = vkPipelineLayout;

		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32_UINT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_UINT, 4);

			var builder = pipelineBuilder(context, stack);
			builder.simpleShaderStages(
					"Color/Gradient", "com/github/knokko/vk2d/",
					"color.vert.spv", "color.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			builder.ciPipeline.layout(vkPipelineLayout);
			modifyPipelineSettings(stack, builder);

			this.vkPipeline = builder.build("Vk2dColorPipeline");
		}
	}

	protected abstract void modifyPipelineSettings(MemoryStack stack, GraphicsPipelineBuilder pipelineBuilder);

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
