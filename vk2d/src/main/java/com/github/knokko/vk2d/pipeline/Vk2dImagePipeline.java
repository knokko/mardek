package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dImagePipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 24;
	private static final int[] BYTES_PER_TRIANGLE = { 3 * VERTEX_SIZE };
	private static final int[] VERTEX_ALIGNMENTS = { VERTEX_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dImagePipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super();

		try (MemoryStack stack = stackPush()) {
			this.vkPipelineLayout = context.boiler().pipelines.createLayout(
					null, "Vk2dImagePipelineLayout",
					instance.imageDescriptorSetLayout.vkDescriptorSetLayout
			);

			var vertexAttributes = VkVertexInputAttributeDescription.calloc(3, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32G32_UINT, 16);

			var builder = pipelineBuilder(context, stack);
			builder.simpleShaderStages(
					"SimpleImage", "com/github/knokko/vk2d/",
					"image.vert.spv", "image.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes, VERTEX_SIZE);
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dImagePipeline");
		}
	}

	public Vk2dImageBatch addBatch(Vk2dRenderStage stage, int initialCapacity, Vk2dResourceBundle bundle) {
		return new Vk2dImageBatch(this, stage, initialCapacity, bundle);
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
	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MiniBatch miniBatch, Vk2dBatch batch) {
		int firstVertex = Math.toIntExact((miniBatch.vertexBuffers()[0].offset - perFrameBuffer.buffer.offset) / VERTEX_SIZE);
		LongBuffer pDescriptorSet = recorder.stack.callocLong(1);
		int descriptorIndex = 0;
		for (int index = 0; index < miniBatch.vertexData()[0].position(); index += 6 * VERTEX_SIZE) {
			// TODO Only switch descriptors when needed
			pDescriptorSet.put(0, ((Vk2dImageBatch) batch).descriptorSets[descriptorIndex]);
			vkCmdBindDescriptorSets(
					recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
					vkPipelineLayout, 0, pDescriptorSet, null
			);
			vkCmdDraw(recorder.commandBuffer, 6, 1, firstVertex, 0);
			firstVertex += 6;
			descriptorIndex += 1;
		}
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
