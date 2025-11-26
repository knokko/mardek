package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dKim3Batch;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dKim3Pipeline extends Vk2dPipeline {

	protected static final int VERTEX_SIZE = 16;
	public static final int INFO_SIZE = 12;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE, INFO_SIZE / 2 };
	private static final int[] VERTEX_ALIGNMENTS = { VERTEX_SIZE, INFO_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dKim3Pipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super(context.printBatchSizes);

		this.vkPipelineLayout = instance.kim3PipelineLayout;
		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(3, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32_UINT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 4);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 12);

			var builder = pipelineBuilder(context, stack);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			setShaderStages(builder);
			builder.ciPipeline.layout(this.vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dKim3Pipeline");
		}
	}

	protected void setShaderStages(GraphicsPipelineBuilder builder) {
		builder.simpleShaderStages(
				"Kim3", "com/github/knokko/vk2d/",
				"kim3.vert.spv", "kim3.frag.spv"
		);
	}

	public Vk2dKim3Batch addBatch(
			Vk2dRenderStage frame, int initialCapacity,
			Vk2dResourceBundle bundle, long perFrameDescriptorSet
	) {
		return new Vk2dKim3Batch(
				this, frame, initialCapacity, bundle, perFrameDescriptorSet
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

		Vk2dKim3Batch kimBatch = (Vk2dKim3Batch) batch;
		recorder.bindGraphicsDescriptors(
				vkPipelineLayout, kimBatch.bundle.fakeImageDescriptorSet,
				kimBatch.perFrameDescriptorSet
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
