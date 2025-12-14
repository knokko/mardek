package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.Vk2dMultiplyBatch;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;

import static org.lwjgl.vulkan.VK10.*;

public class Vk2dMultiplyPipeline extends Vk2dAbstractColorPipeline {

	public Vk2dMultiplyPipeline(Vk2dPipelineContext context, Vk2dInstance instance) {
		super(context, instance.colorPipelineLayout);
	}

	/**
	 * @param initialCapacity The initial capacity of this batch, in <i>triangles</i>
	 */
	public Vk2dMultiplyBatch addBatch(Vk2dRenderStage stage, int initialCapacity) {
		return new Vk2dMultiplyBatch(this, stage, initialCapacity);
	}

	@Override
	protected void modifyPipelineSettings(MemoryStack stack, GraphicsPipelineBuilder pipelineBuilder) {
		var blendAttachments = VkPipelineColorBlendAttachmentState.calloc(1, stack);
		var blend = blendAttachments.get(0);
		blend.blendEnable(true);
		blend.srcColorBlendFactor(VK_BLEND_FACTOR_ZERO);
		blend.dstColorBlendFactor(VK_BLEND_FACTOR_SRC_COLOR);
		blend.colorBlendOp(VK_BLEND_OP_ADD);
		blend.srcAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
		blend.dstAlphaBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
		blend.alphaBlendOp(VK_BLEND_OP_ADD);
		blend.colorWriteMask(
				VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
						VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT
		);

		var ciBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack);
		ciBlend.sType$Default();
		ciBlend.attachmentCount(1);
		ciBlend.pAttachments(blendAttachments);

		pipelineBuilder.ciPipeline.pColorBlendState(ciBlend);
	}
}
