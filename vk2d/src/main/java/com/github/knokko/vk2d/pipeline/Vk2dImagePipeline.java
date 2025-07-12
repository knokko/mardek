package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.Vk2dDescriptors;
import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dImagePipeline extends Vk2dPipeline<Vk2dImageBatch> {

	public static final int VERTEX_SIZE = 16;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dImagePipeline(PipelineContext context, Vk2dDescriptors descriptors) {
		super(VERTEX_SIZE);

		try (MemoryStack stack = stackPush()) {
			this.vkPipelineLayout = context.boiler().pipelines.createLayout(
					null, "Vk2dImagePipelineLayout",
					descriptors.imageLayout.vkDescriptorSetLayout
			);

			var vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"SimpleImage", "com/github/knokko/vk2d/",
					"image.vert.spv", "image.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dImagePipeline");
		}
	}

	@Override
	public Vk2dImageBatch createBatch(PerFrameBuffer perFrameBuffer, int initialCapacity, int width, int height) {
		return new Vk2dImageBatch(this, perFrameBuffer, initialCapacity, width, height);
	}

	@Override
	public void recordBatch(CommandRecorder recorder, PerFrameBuffer perFrameBuffer, MappedVkbBuffer vertexData) {
		int firstVertex = Math.toIntExact((vertexData.offset - perFrameBuffer.buffer.offset) / vertexSize);
		ByteBuffer dataBuffer = vertexData.byteBuffer();
		LongBuffer pDescriptorSet = recorder.stack.callocLong(1);
		for (int index = 0; index < vertexData.size; index += 7 * VERTEX_SIZE) {
			pDescriptorSet.put(0, dataBuffer.getLong(index + 6 * VERTEX_SIZE));
			vkCmdBindDescriptorSets(
					recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
					vkPipelineLayout, 0, pDescriptorSet, null
			);
			vkCmdDraw(recorder.commandBuffer, 6, 1, firstVertex, 0);
			firstVertex += 5;
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
