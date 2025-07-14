package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.Vk2dShared;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dTextBatch;
import com.github.knokko.vk2d.resource.Vk2dFont;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dTextPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 20;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dTextPipeline(Vk2dPipelineContext context, Vk2dShared shared) {
		super(VERTEX_SIZE);

		try (MemoryStack stack = stackPush()) {
			this.vkPipelineLayout = context.boiler().pipelines.createLayout(
					null, "Vk2dTextPipelineLayout",
					shared.textDescriptorSetLayout.vkDescriptorSetLayout
			);

			var vertexAttributes = VkVertexInputAttributeDescription.calloc(3, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 16);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Text", "com/github/knokko/vk2d/",
					"text.vert.spv", "text.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dTextPipeline");
		}
	}

	public Vk2dTextBatch addBatch(Vk2dFrame frame, int initialCapacity, Vk2dFont font) {
		return new Vk2dTextBatch(this, frame, initialCapacity, font);
	}

	@Override
	public void prepareRecording(CommandRecorder recorder, Vk2dBatch batch) {
		super.prepareRecording(recorder, batch);
		recorder.bindGraphicsDescriptors(vkPipelineLayout, ((Vk2dTextBatch) batch).font.vkDescriptorSet);
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
