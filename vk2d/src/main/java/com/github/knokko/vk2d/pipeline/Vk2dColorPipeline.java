package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dColorPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 8;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE };
	private static final int[] VERTEX_ALIGNMENTS = { VERTEX_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dColorPipeline(Vk2dPipelineContext context) {
		super(context.printBatchSizes);

		try (MemoryStack stack = stackPush()) {
			VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8);

			this.vkPipelineLayout = context.boiler.pipelines.createLayout(
					pushConstants, "Vk2dColorPipelineLayout"
			);

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

			this.vkPipeline = builder.build("Vk2dColorPipeline");
		}
	}

	/**
	 * @param initialCapacity The initial capacity of this batch, in <i>triangles</i>
	 */
	public Vk2dColorBatch addBatch(Vk2dRenderStage stage, int initialCapacity) {
		return new Vk2dColorBatch(this, stage, initialCapacity);
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
		vkCmdPushConstants(
				recorder.commandBuffer, vkPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT,
				0, recorder.stack.ints(batch.width, batch.height)
		);
	}

	@Override
	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MiniBatch miniBatch, Vk2dBatch batch) {
		recordNonIndexedBatch(recorder, perFrameBuffer, miniBatch);
	}

	@Override
	public void destroy(BoilerInstance boiler) {
		super.destroy(boiler);
		try (MemoryStack stack = stackPush()) {
			vkDestroyPipelineLayout(
					boiler.vkDevice(), vkPipelineLayout,
					CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler)
			);
		}
	}
}
