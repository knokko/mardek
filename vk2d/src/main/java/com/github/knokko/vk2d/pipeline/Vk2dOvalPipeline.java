package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dOvalBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dOvalPipeline extends Vk2dPipeline {

	private static final int VERTEX_SIZE = 8;
	public static final int OVAL_SIZE = 4 * 16;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE, OVAL_SIZE / 2 };
	private static final int[] VERTEX_ALIGNMENTS = { VERTEX_SIZE, OVAL_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dOvalPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super();
		this.vkPipelineLayout = instance.kimPipelineLayout;

		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32_UINT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32_UINT, 4);

			var builder = pipelineBuilder(context, stack);
			builder.simpleShaderStages(
					"Oval", "com/github/knokko/vk2d/",
					"oval.vert.spv", "oval.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			builder.ciPipeline.layout(instance.kimPipelineLayout);

			this.vkPipeline = builder.build("Vk2dOvalPipeline");
		}
	}

	public Vk2dOvalBatch addBatch(Vk2dRenderStage stage, long perFrameDescriptorSet, int initialCapacity) {
		return new Vk2dOvalBatch(this, stage, perFrameDescriptorSet, initialCapacity);
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
		recorder.bindGraphicsDescriptors(vkPipelineLayout, ((Vk2dOvalBatch) batch).perFrameDescriptorSet);
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
