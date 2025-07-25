package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dTextBatch;
import com.github.knokko.vk2d.resource.Vk2dFont;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dTextPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 28;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dTextPipeline(Vk2dPipelineContext context, Vk2dShared shared) {
		super(VERTEX_SIZE);

		this.vkPipelineLayout = shared.kimPipelineLayout;
		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(5, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 16);
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_UINT, 20);
			vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32_UINT, 24);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Text", "com/github/knokko/vk2d/",
					"text.vert.spv", "text.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(shared.kimPipelineLayout);

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
}
