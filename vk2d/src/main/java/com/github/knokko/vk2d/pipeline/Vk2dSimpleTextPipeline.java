package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.MiniBatch;
import com.github.knokko.vk2d.batch.Vk2dBatch;
import com.github.knokko.vk2d.batch.Vk2dSimpleTextBatch;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.text.Vk2dTextStyleCache;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPushConstantRange;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dSimpleTextPipeline extends Vk2dPipeline {

	public static final int QUAD_SIZE = 36;
	private static final int[] BYTES_PER_TRIANGLE = { QUAD_SIZE / 2 };
	private static final int[] VERTEX_ALIGNMENTS = { QUAD_SIZE };

	private final long vkPipelineLayout;

	public Vk2dSimpleTextPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super(context.printBatchSizes);

		try (MemoryStack stack = stackPush()) {
			var pushConstants = VkPushConstantRange.calloc(1, stack);
			pushConstants.get(0).set(
					VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, 16
			);

			this.vkPipelineLayout = context.boiler.pipelines.createLayout(
					pushConstants, "Vk2dSimpleTextPipelineLayout",
					instance.bufferDescriptorSetLayout.vkDescriptorSetLayout,
					instance.bufferDescriptorSetLayout.vkDescriptorSetLayout,
					instance.imageDescriptorSetLayout.vkDescriptorSetLayout
			);

			var builder = pipelineBuilder(context, stack);
			builder.simpleShaderStages(
					"SimpleText", "com/github/knokko/vk2d/text/",
					"simple.vert.spv", "simple.frag.spv"
			);
			builder.noVertexInput();
			builder.ciPipeline.layout(vkPipelineLayout);

			this.vkPipeline = builder.build("Vk2dSimpleTextPipeline");
		}
	}

	public Vk2dSimpleTextBatch addBatch(Vk2dRenderStage stage, int initialCapacity, Vk2dTextStyleCache cache) {
		return new Vk2dSimpleTextBatch(this, stage, initialCapacity, cache);
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
		Vk2dSimpleTextBatch simpleBatch = (Vk2dSimpleTextBatch) batch;
		var pushConstants = recorder.stack.callocFloat(4);

		recordDescriptorPerDrawBatch(
				recorder, perFrameBuffer, miniBatch, batch, QUAD_SIZE / 6,
				quadIndex -> simpleBatch.atlases[quadIndex], atlas -> {
					recorder.bindGraphicsDescriptors(
							vkPipelineLayout,
							simpleBatch.cache.perFrameDescriptorSet,
							simpleBatch.cache.perFrameDescriptorSet,
							atlas.getRenderDescriptorSet()
					);
					pushConstants.put(0, atlas.getWidth());
					pushConstants.put(1, atlas.getHeight());
					pushConstants.put(2, atlas.heightA);
					pushConstants.put(3, atlas.distanceScale);
					vkCmdPushConstants(
							recorder.commandBuffer, vkPipelineLayout,
							VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, pushConstants
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
