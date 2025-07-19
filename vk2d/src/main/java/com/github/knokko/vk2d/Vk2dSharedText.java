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

	public final VkbDescriptorSetLayout scratchDescriptorLayout, transferDescriptorLayout, intersectionDescriptorLayout;
	public final long scratchPipeline, scratchPipelineLayout, transferPipeline, transferPipelineLayout, intersectionPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dSharedText(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 3);
			descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(2, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			this.scratchDescriptorLayout = descriptors.build(boiler, "Vk2dTextScratchDescriptorLayout");

			descriptors = new DescriptorSetLayoutBuilder(stack, 5);
			descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(2, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(3, 3, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			descriptors.set(4, 4, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
			this.transferDescriptorLayout = descriptors.build(boiler, "Vk2dTextTransferDescriptorLayout");

			descriptors = new DescriptorSetLayoutBuilder(stack, 2);
			descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
			descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.intersectionDescriptorLayout = descriptors.build(boiler, "Vk2dTextIntersectionDescriptorLayout");

			VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
			pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 28);
			this.scratchPipelineLayout = boiler.pipelines.createLayout(
					pushConstants, "Vk2dTextScratchPipelineLayout",
					scratchDescriptorLayout.vkDescriptorSetLayout
			);
			pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 20);
			this.transferPipelineLayout = boiler.pipelines.createLayout(
					pushConstants, "Vk2dTextTransferPipelineLayout",
					transferDescriptorLayout.vkDescriptorSetLayout
			);
			this.intersectionPipelineLayout = boiler.pipelines.createLayout(
					null, "Vk2dIntersectionPipelineLayout",
					intersectionDescriptorLayout.vkDescriptorSetLayout
			);

			this.scratchPipeline = boiler.pipelines.createComputePipeline(
					scratchPipelineLayout,
					"com/github/knokko/vk2d/glyph-scratch.comp.spv",
					"Vk2dTextScratchPipeline"
			);
			this.transferPipeline = boiler.pipelines.createComputePipeline(
					transferPipelineLayout,
					"com/github/knokko/vk2d/glyph-transfer.comp.spv",
					"Vk2dTextTransferPipeline"
			);
		}
	}

	public void destroy(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			vkDestroyPipeline(boiler.vkDevice(), scratchPipeline, CallbackUserData.PIPELINE.put(stack, boiler));
			vkDestroyPipeline(boiler.vkDevice(), transferPipeline, CallbackUserData.PIPELINE.put(stack, boiler));
			for (long layout : new long[] { scratchPipelineLayout, transferPipelineLayout, intersectionPipelineLayout }) {
				vkDestroyPipelineLayout(boiler.vkDevice(), layout, CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler));
			}

			for (VkbDescriptorSetLayout layout : new VkbDescriptorSetLayout[] {
					scratchDescriptorLayout, transferDescriptorLayout, intersectionDescriptorLayout
			}) {
				vkDestroyDescriptorSetLayout(
						boiler.vkDevice(), layout.vkDescriptorSetLayout,
						CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
				);
			}
		}
	}
}
