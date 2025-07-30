package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.batch.BatchVertexData;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dOvalBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dOvalPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 60;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE };
	private static final int[] VERTEX_ALIGNMENTS = { VERTEX_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dOvalPipeline(Vk2dPipelineContext context) {
		super(BYTES_PER_TRIANGLE, VERTEX_ALIGNMENTS);

		try (MemoryStack stack = stackPush()) {
			this.vkPipelineLayout = context.boiler().pipelines.createLayout(
					null, "Vk2dOvalPipelineLayout"
			);

			var vertexAttributes = VkVertexInputAttributeDescription.calloc(6, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32G32_SFLOAT, 16);
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_UINT, 24);
			vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32G32B32A32_UINT, 28);
			vertexAttributes.get(5).set(5, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 44);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Oval", "com/github/knokko/vk2d/",
					"oval.vert.spv", "oval.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dOvalPipeline");
		}
	}

	public Vk2dOvalBatch addBatch(Vk2dFrame frame, int initialCapacity) {
		return new Vk2dOvalBatch(this, frame, initialCapacity);
	}

	@Override
	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, BatchVertexData miniBatch, Vk2dBatch batch) {
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
