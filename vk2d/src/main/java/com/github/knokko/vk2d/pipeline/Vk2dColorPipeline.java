package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dColorPipeline extends Vk2dPipeline<Vk2dColorBatch> {

	public static final int VERTEX_SIZE = 24;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dColorPipeline(PipelineContext context) {
		super(VERTEX_SIZE);

		try (MemoryStack stack = stackPush()) {
			VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8);

			this.vkPipelineLayout = context.boiler().pipelines.createLayout(
					pushConstants, "Vk2dColorPipelineLayout"
			);

			var vertexAttributes = VkVertexInputAttributeDescription.calloc(2, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 8);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Color/Gradient", "com/github/knokko/vk2d/",
					"color.vert.spv", "color.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dColorPipeline");
		}
	}

	@Override
	public Vk2dColorBatch createBatch(PerFrameBuffer perFrameBuffer, int initialCapacity, int width, int height) {
		return new Vk2dColorBatch(this, perFrameBuffer, initialCapacity, width, height);
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
