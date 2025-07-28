package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dGlyphPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 64;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dGlyphPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super(VERTEX_SIZE);

		this.vkPipelineLayout = instance.textIntersectionPipelineLayout;
		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(11, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32G32_SFLOAT, 16);
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_UINT, 24);
			vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32_UINT, 28);
			vertexAttributes.get(5).set(5, 0, VK_FORMAT_R32G32_SFLOAT, 32);
			vertexAttributes.get(6).set(6, 0, VK_FORMAT_R32G32_UINT, 40);
			vertexAttributes.get(7).set(7, 0, VK_FORMAT_R32_UINT, 48);
			vertexAttributes.get(8).set(8, 0, VK_FORMAT_R32_UINT, 52);
			vertexAttributes.get(9).set(9, 0, VK_FORMAT_R32_UINT, 56);
			vertexAttributes.get(10).set(10, 0, VK_FORMAT_R32_SFLOAT, 60);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Glyph", "com/github/knokko/vk2d/",
					"glyph.vert.spv", "glyph.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(instance.textIntersectionPipelineLayout);

			this.vkPipeline = builder.build("Vk2dGlyphPipeline");
		}
	}

	public Vk2dGlyphBatch addBatch(Vk2dFrame frame, int initialCapacity, CommandRecorder recorder, Vk2dTextBuffer textBuffer) {
		return new Vk2dGlyphBatch(
				this, frame, initialCapacity, recorder, textBuffer
		);
	}

	@Override
	public void prepareRecording(CommandRecorder recorder, Vk2dBatch batch) {
		super.prepareRecording(recorder, batch);
		recorder.bindGraphicsDescriptors(vkPipelineLayout, ((Vk2dGlyphBatch) batch).textBuffer.getRenderDescriptorSet());
	}
}
