package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dFancyTextBatch;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.text.Vk2dFancyTextStyleCache;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPushConstantRange;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;

public class Vk2dFancyTextPipeline extends Vk2dPipeline {

	public static final int QUAD_SIZE = 60;
	private static final int[] BYTES_PER_TRIANGLE = { QUAD_SIZE / 2 };
	private static final int[] VERTEX_ALIGNMENTS = { QUAD_SIZE };

	private final long vkPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dFancyTextPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super(false, true, context.printBatchSizes);

		try (MemoryStack stack = stackPush()) {
			var pushConstants = VkPushConstantRange.calloc(2, stack);
			pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 16);
			pushConstants.get(1).set(VK_SHADER_STAGE_FRAGMENT_BIT, 16, 8);

			this.vkPipelineLayout = context.boiler.pipelines.createLayout(
					pushConstants, "Vk2dFancyTextPipelineLayout",
					instance.bufferDescriptorSetLayout.vkDescriptorSetLayout,
					instance.bufferDescriptorSetLayout.vkDescriptorSetLayout,
					instance.imageDescriptorSetLayout.vkDescriptorSetLayout
			);

			var builder = pipelineBuilder(context, stack);
			builder.simpleShaderStages(
					"FancyText", "com/github/knokko/vk2d/text/",
					"fancy.vert.spv", "fancy.frag.spv"
			);
			builder.noVertexInput();
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dFancyTextPipeline");
		}
	}

	public Vk2dFancyTextBatch addBatch(Vk2dRenderStage stage, int initialCapacity, Vk2dFancyTextStyleCache cache) {
		return new Vk2dFancyTextBatch(this, stage, initialCapacity, cache);
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
		Vk2dFancyTextBatch fancyBatch = (Vk2dFancyTextBatch) batch;
		var vertexPushConstants = recorder.stack.callocInt(4);
		var fragmentPushConstants = recorder.stack.callocFloat(2);

		recordDescriptorPerDrawBatch(
				recorder, perFrameBuffer, miniBatch, batch, QUAD_SIZE / 6,
				quadIndex -> fancyBatch.atlases[quadIndex], atlas -> {
					recorder.bindGraphicsDescriptors(
							vkPipelineLayout,
							fancyBatch.cache.perFrameDescriptorSet,
							fancyBatch.cache.perFrameDescriptorSet,
							atlas.getRenderDescriptorSet()
					);
					vertexPushConstants.put(atlas.getWidth());
					vertexPushConstants.put(atlas.getHeight());
					vertexPushConstants.put(batch.width);
					vertexPushConstants.put(batch.height);
					fragmentPushConstants.put(atlas.heightA);
					fragmentPushConstants.put(atlas.distanceScale);

					vertexPushConstants.position(0);
					vkCmdPushConstants(
							recorder.commandBuffer, vkPipelineLayout,
							VK_SHADER_STAGE_VERTEX_BIT, 0, vertexPushConstants
					);

					fragmentPushConstants.position(0);
					vkCmdPushConstants(
							recorder.commandBuffer, vkPipelineLayout,
							VK_SHADER_STAGE_FRAGMENT_BIT, 16, fragmentPushConstants
					);
				}
		);
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
