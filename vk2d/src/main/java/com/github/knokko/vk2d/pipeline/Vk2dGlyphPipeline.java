package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.Vk2dSharedText;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.resource.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dGlyphPipeline extends Vk2dPipeline {

	public static final int VERTEX_SIZE = 44;

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dGlyphPipeline(Vk2dPipelineContext context, Vk2dSharedText shared) {
		super(VERTEX_SIZE);

		this.vkPipelineLayout = shared.intersectionPipelineLayout;
		try (MemoryStack stack = stackPush()) {
			var vertexAttributes = VkVertexInputAttributeDescription.calloc(8, stack);
			vertexAttributes.get(0).set(0, 0, VK_FORMAT_R32G32_SFLOAT, 0);
			vertexAttributes.get(1).set(1, 0, VK_FORMAT_R32G32_SFLOAT, 8);
			vertexAttributes.get(2).set(2, 0, VK_FORMAT_R32_UINT, 16);
			vertexAttributes.get(3).set(3, 0, VK_FORMAT_R32_UINT, 20);
			vertexAttributes.get(4).set(4, 0, VK_FORMAT_R32G32_UINT, 24);
			vertexAttributes.get(5).set(5, 0, VK_FORMAT_R32_UINT, 32);
			vertexAttributes.get(6).set(6, 0, VK_FORMAT_R32_UINT, 36);
			vertexAttributes.get(7).set(7, 0, VK_FORMAT_R32_UINT, 40);

			var builder = pipelineBuilder(context);
			builder.simpleShaderStages(
					"Glyph", "com/github/knokko/vk2d/",
					"glyph.vert.spv", "glyph.frag.spv"
			);
			simpleVertexInput(builder, stack, vertexAttributes);
			builder.ciPipeline.layout(shared.intersectionPipelineLayout);

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
