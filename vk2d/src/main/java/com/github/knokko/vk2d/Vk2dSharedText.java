package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPushConstantRange;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dSharedText {

	public final VkbDescriptorSetLayout scratchDescriptorLayout;
	public final long scratchPipeline, scratchPipelineLayout;

	public Vk2dSharedText(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 3);
			descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(2, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			this.scratchDescriptorLayout = descriptors.build(boiler, "Vk2dTextScratchDescriptorLayout");

			VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
			pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 28);

			this.scratchPipelineLayout = boiler.pipelines.createLayout(
					pushConstants, "Vk2dTextScratchPipelineLayout",
					scratchDescriptorLayout.vkDescriptorSetLayout
			);

			this.scratchPipeline = boiler.pipelines.createComputePipeline(
					scratchPipelineLayout,
					"com/github/knokko/vk2d/glyph-scratch.comp.spv",
					"Vk2dTextScratchPipeline"
			);
		}
	}

	public void destroy(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			vkDestroyPipeline(boiler.vkDevice(), scratchPipeline, CallbackUserData.PIPELINE.put(stack, boiler));
			vkDestroyPipelineLayout(
					boiler.vkDevice(), scratchPipelineLayout,
					CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler)
			);
			vkDestroyDescriptorSetLayout(
					boiler.vkDevice(), scratchDescriptorLayout.vkDescriptorSetLayout,
					CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
			);
		}
	}
}
