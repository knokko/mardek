package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPushConstantRange;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;

public class Vk2dInstance {

	public final BoilerInstance boiler;

	public final long pixelatedSampler;
	public final long smoothSampler;

	public final VkbDescriptorSetLayout imageDescriptorSetLayout;
	public final VkbDescriptorSetLayout bufferDescriptorSetLayout;

	public final long kimPipelineLayout;

	public final VkbDescriptorSetLayout textScratchDescriptorLayout0, textScratchDescriptorLayout1;
	public final VkbDescriptorSetLayout textTransferDescriptorLayout, textIntersectionDescriptorLayout;
	public final long textScratchPipeline, textScratchPipelineLayout;
	public final long textTransferPipeline, textTransferPipelineLayout;
	public final long textIntersectionPipelineLayout;

	@SuppressWarnings("resource")
	public Vk2dInstance(BoilerInstance boiler, Vk2dConfig config) {
		this.boiler = boiler;
		try (MemoryStack stack = stackPush()) {
			if (config.image) {
				this.pixelatedSampler = boiler.images.createSimpleSampler(
						VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
						VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "Vk2dPixelatedSampler"
				);
				this.smoothSampler = boiler.images.createSimpleSampler(
						VK_FILTER_LINEAR, VK_SAMPLER_MIPMAP_MODE_LINEAR,
						VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "Vk2dSmoothSampler"
				);
				DescriptorSetLayoutBuilder builder = new DescriptorSetLayoutBuilder(stack, 1);
				builder.set(0, 0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
				this.imageDescriptorSetLayout = builder.build(boiler, "Vk2dImageDescriptorLayout");
			} else {
				this.pixelatedSampler = VK_NULL_HANDLE;
				this.smoothSampler = VK_NULL_HANDLE;
				this.imageDescriptorSetLayout = null;
			}

			if (config.shouldCreateBufferPipelineLayout()) {
				DescriptorSetLayoutBuilder builder = new DescriptorSetLayoutBuilder(stack, 1);
				builder.set(
						0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
						VK_SHADER_STAGE_FRAGMENT_BIT | VK_SHADER_STAGE_VERTEX_BIT
				);
				this.bufferDescriptorSetLayout = builder.build(boiler, "Vk2dBufferDescriptorLayout");

				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8);

				this.kimPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dKimPipelineLayout",
						bufferDescriptorSetLayout.vkDescriptorSetLayout
				);
			} else {
				this.bufferDescriptorSetLayout = null;
				this.kimPipelineLayout = VK_NULL_HANDLE;
			}

			if (config.text) {
				DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 2);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				this.textScratchDescriptorLayout0 = descriptors.build(boiler, "Vk2dTextScratchDescriptorLayout0");

				descriptors = new DescriptorSetLayoutBuilder(stack, 1);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				this.textScratchDescriptorLayout1 = descriptors.build(boiler, "Vk2dTextScratchDescriptorLayout1");

				descriptors = new DescriptorSetLayoutBuilder(stack, 6);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(2, 2, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(3, 3, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(4, 4, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(5, 5, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				this.textTransferDescriptorLayout = descriptors.build(boiler, "Vk2dTextTransferDescriptorLayout");

				descriptors = new DescriptorSetLayoutBuilder(stack, 2);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
				descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
				this.textIntersectionDescriptorLayout = descriptors.build(boiler, "Vk2dTextIntersectionDescriptorLayout");

				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 40);
				this.textScratchPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dTextScratchPipelineLayout",
						textScratchDescriptorLayout0.vkDescriptorSetLayout,
						textScratchDescriptorLayout1.vkDescriptorSetLayout
				);
				pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 16);
				this.textTransferPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dTextTransferPipelineLayout",
						textTransferDescriptorLayout.vkDescriptorSetLayout
				);
				this.textIntersectionPipelineLayout = boiler.pipelines.createLayout(
						null, "Vk2dIntersectionPipelineLayout",
						textIntersectionDescriptorLayout.vkDescriptorSetLayout
				);

				this.textScratchPipeline = boiler.pipelines.createComputePipeline(
						textScratchPipelineLayout,
						"com/github/knokko/vk2d/glyph/scratch.comp.spv",
						"Vk2dTextScratchPipeline"
				);
				this.textTransferPipeline = boiler.pipelines.createComputePipeline(
						textTransferPipelineLayout,
						"com/github/knokko/vk2d/glyph/transfer.comp.spv",
						"Vk2dTextTransferPipeline"
				);
			} else {
				this.textScratchDescriptorLayout0 = null;
				this.textScratchDescriptorLayout1 = null;
				this.textTransferDescriptorLayout = null;
				this.textIntersectionDescriptorLayout = null;
				this.textScratchPipelineLayout = VK_NULL_HANDLE;
				this.textTransferPipelineLayout = VK_NULL_HANDLE;
				this.textIntersectionPipelineLayout = VK_NULL_HANDLE;
				this.textScratchPipeline = VK_NULL_HANDLE;
				this.textTransferPipeline = VK_NULL_HANDLE;
			}
		}
	}

	public void destroy() {
		try (MemoryStack stack = stackPush()) {
			vkDestroySampler(boiler.vkDevice(), pixelatedSampler, CallbackUserData.SAMPLER.put(stack, boiler));
			vkDestroySampler(boiler.vkDevice(), smoothSampler, CallbackUserData.SAMPLER.put(stack, boiler));
			if (imageDescriptorSetLayout != null) {
				vkDestroyDescriptorSetLayout(
						boiler.vkDevice(), imageDescriptorSetLayout.vkDescriptorSetLayout,
						CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
				);
			}
			if (bufferDescriptorSetLayout != null) {
				vkDestroyDescriptorSetLayout(
						boiler.vkDevice(), bufferDescriptorSetLayout.vkDescriptorSetLayout,
						CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
				);
			}
			vkDestroyPipelineLayout(
					boiler.vkDevice(), kimPipelineLayout,
					CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler)
			);

			vkDestroyPipeline(boiler.vkDevice(), textScratchPipeline, CallbackUserData.PIPELINE.put(stack, boiler));
			vkDestroyPipeline(boiler.vkDevice(), textTransferPipeline, CallbackUserData.PIPELINE.put(stack, boiler));
			for (long layout : new long[] {
					textScratchPipelineLayout, textTransferPipelineLayout, textIntersectionPipelineLayout
			}) {
				vkDestroyPipelineLayout(boiler.vkDevice(), layout, CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler));
			}

			for (VkbDescriptorSetLayout layout : new VkbDescriptorSetLayout[] {
					textScratchDescriptorLayout0, textScratchDescriptorLayout1,
					textTransferDescriptorLayout, textIntersectionDescriptorLayout
			}) {
				if (layout == null) continue;
				vkDestroyDescriptorSetLayout(
						boiler.vkDevice(), layout.vkDescriptorSetLayout,
						CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
				);
			}
		}
	}
}
