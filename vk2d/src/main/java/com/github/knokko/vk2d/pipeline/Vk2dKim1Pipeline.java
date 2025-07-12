package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.Vk2dShared;
import com.github.knokko.vk2d.batch.Vk2dKim1Batch;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dKim1Pipeline extends Vk2dPipeline<Vk2dKim1Batch> {

	public static final int VERTEX_SIZE = 20;

	private final long vkPipelineLayout;
	private final long descriptorSet;

	@SuppressWarnings("resource")
	public Vk2dKim1Pipeline(Vk2dPipelineContext context, Vk2dResourceBundle bundle, Vk2dShared shared) {
		super(VERTEX_SIZE);

		this.descriptorSet = bundle.fakeImageDescriptorSet;
		try (MemoryStack stack = stackPush()) {
			this.vkPipelineLayout = context.boiler().pipelines.createLayout(
					null, "Vk2dKim1PipelineLayout",
					shared.bufferDescriptorSetLayout.vkDescriptorSetLayout
			);

			var vertexAttributes = VkVertexInputAttributeDescription.calloc(3, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 16);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Kim1", "com/github/knokko/vk2d/",
					"kim1.vert.spv", "kim1.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dKim1Pipeline");
		}
	}

	@Override
	public Vk2dKim1Batch createBatch(PerFrameBuffer perFrameBuffer, int initialCapacity, int width, int height) {
		return new Vk2dKim1Batch(this, perFrameBuffer, initialCapacity, width, height);
	}

	@Override
	public void prepareRecording(CommandRecorder recorder, int viewportWidth, int viewportHeight) {
		super.prepareRecording(recorder, viewportWidth, viewportHeight);
		recorder.bindGraphicsDescriptors(vkPipelineLayout, descriptorSet);
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
