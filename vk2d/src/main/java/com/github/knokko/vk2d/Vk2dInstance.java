package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkPushConstantRange;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dInstance {

	public final BoilerInstance boiler;

	public final long pixelatedSampler;
	public final long smoothSampler;

	public final VkbDescriptorSetLayout imageDescriptorSetLayout;
	public final VkbDescriptorSetLayout bufferDescriptorSetLayout;
	public final VkbDescriptorSetLayout doubleComputeBufferDescriptorLayout;

	public final long kimPipelineLayout;

	public final VkbDescriptorSetLayout textScratchDescriptorLayout1;
	public final VkbDescriptorSetLayout textTransferDescriptorLayout, textIntersectionDescriptorLayout;
	public final VkbDescriptorSetLayout blurDescriptorLayout1;
	public final long textScratchPipeline, textScratchPipelineLayout;
	public final long textTransferPipeline, textTransferPipelineLayout;
	public final long textIntersectionPipelineLayout;
	public final long blurPipelineLayout1, blurPipelineLayout2, blurPipelineLayoutSample;
	public final long blurPipeline1, blurPipeline2;

	@SuppressWarnings("resource")
	public Vk2dInstance(BoilerInstance boiler, Vk2dConfig config) {
		this.boiler = boiler;
		try (MemoryStack stack = stackPush()) {
			if (config.image || config.blur) {
				this.pixelatedSampler = boiler.images.createSimpleSampler(
						VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
						VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "Vk2dPixelatedSampler"
				);
			} else {
				this.pixelatedSampler = VK_NULL_HANDLE;
			}
			if (config.image) {
				this.smoothSampler = boiler.images.createSimpleSampler(
						VK_FILTER_LINEAR, VK_SAMPLER_MIPMAP_MODE_LINEAR,
						VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, "Vk2dSmoothSampler"
				);
				DescriptorSetLayoutBuilder builder = new DescriptorSetLayoutBuilder(stack, 1);
				builder.set(0, 0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
				this.imageDescriptorSetLayout = builder.build(boiler, "Vk2dImageDescriptorLayout");
			} else {
				this.smoothSampler = VK_NULL_HANDLE;
				this.imageDescriptorSetLayout = null;
			}

			if (config.shouldCreateBufferPipelineLayout() || config.text || config.blur) {
				DescriptorSetLayoutBuilder builder = new DescriptorSetLayoutBuilder(stack, 1);
				builder.set(
						0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
						VK_SHADER_STAGE_FRAGMENT_BIT | VK_SHADER_STAGE_VERTEX_BIT
				);
				this.bufferDescriptorSetLayout = builder.build(boiler, "Vk2dBufferDescriptorLayout");
			} else {
				this.bufferDescriptorSetLayout = null;
			}

			if (config.shouldCreateBufferPipelineLayout()) {
				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8);

				assert bufferDescriptorSetLayout != null;
				this.kimPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dKimPipelineLayout",
						bufferDescriptorSetLayout.vkDescriptorSetLayout
				);
			} else {
				this.kimPipelineLayout = VK_NULL_HANDLE;
			}

			if (config.text || config.blur) {
				DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 2);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				this.doubleComputeBufferDescriptorLayout = descriptors.build(
						boiler, "Vk2dDoubleComputeBufferDescriptorLayout"
				);
			} else {
				this.doubleComputeBufferDescriptorLayout = null;
			}

			if (config.text) {
				DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 1);
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
				pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 48);
				this.textScratchPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dTextScratchPipelineLayout",
						doubleComputeBufferDescriptorLayout.vkDescriptorSetLayout,
						textScratchDescriptorLayout1.vkDescriptorSetLayout
				);
				pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 16);
				this.textTransferPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dTextTransferPipelineLayout",
						textTransferDescriptorLayout.vkDescriptorSetLayout
				);
				pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8);
				assert bufferDescriptorSetLayout != null;
				this.textIntersectionPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dIntersectionPipelineLayout",
						textIntersectionDescriptorLayout.vkDescriptorSetLayout,
						bufferDescriptorSetLayout.vkDescriptorSetLayout
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
				this.textScratchDescriptorLayout1 = null;
				this.textTransferDescriptorLayout = null;
				this.textIntersectionDescriptorLayout = null;
				this.textScratchPipelineLayout = VK_NULL_HANDLE;
				this.textTransferPipelineLayout = VK_NULL_HANDLE;
				this.textIntersectionPipelineLayout = VK_NULL_HANDLE;
				this.textScratchPipeline = VK_NULL_HANDLE;
				this.textTransferPipeline = VK_NULL_HANDLE;
			}

			if (config.blur) {
				DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 2);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_COMPUTE_BIT);
				this.blurDescriptorLayout1 = descriptors.build(boiler, "BlurDescriptorLayoutStage1");

				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 16);
				this.blurPipelineLayout1 = boiler.pipelines.createLayout(
						pushConstants, "BlurPipelineLayoutStage1", blurDescriptorLayout1.vkDescriptorSetLayout
				);
				this.blurPipelineLayout2 = boiler.pipelines.createLayout(
						pushConstants, "BlurPipelineLayoutStage2",
						doubleComputeBufferDescriptorLayout.vkDescriptorSetLayout
				);

				pushConstants = VkPushConstantRange.calloc(2, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_FRAGMENT_BIT, 0, 8);
				pushConstants.get(1).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 24);
				assert bufferDescriptorSetLayout != null;
				this.blurPipelineLayoutSample = boiler.pipelines.createLayout(
						pushConstants, "BlurPipelineLayoutSample",
						bufferDescriptorSetLayout.vkDescriptorSetLayout
				);

				String shaderPath = "com/github/knokko/vk2d/blur/";
				this.blurPipeline1 = boiler.pipelines.createComputePipeline(
						this.blurPipelineLayout1, shaderPath + "stage1.comp.spv", "Vk2dBlurStage1"
				);
				this.blurPipeline2 = boiler.pipelines.createComputePipeline(
						this.blurPipelineLayout2, shaderPath + "stage2.comp.spv", "Vk2dBlurStage2"
				);
			} else {
				this.blurDescriptorLayout1 = null;
				this.blurPipelineLayout1 = VK_NULL_HANDLE;
				this.blurPipelineLayout2 = VK_NULL_HANDLE;
				this.blurPipelineLayoutSample = VK_NULL_HANDLE;
				this.blurPipeline1 = VK_NULL_HANDLE;
				this.blurPipeline2 = VK_NULL_HANDLE;
			}
		}
	}

	public void destroy() {
		try (MemoryStack stack = stackPush()) {
			VkAllocationCallbacks samplerCallbacks = CallbackUserData.SAMPLER.put(stack, boiler);
			vkDestroySampler(boiler.vkDevice(), pixelatedSampler, samplerCallbacks);
			vkDestroySampler(boiler.vkDevice(), smoothSampler, samplerCallbacks);

			VkAllocationCallbacks pipelineCallbacks = CallbackUserData.PIPELINE.put(stack, boiler);
			long[] pipelines = { textScratchPipeline, textTransferPipeline, blurPipeline1, blurPipeline2 };
			for (long pipeline : pipelines) vkDestroyPipeline(boiler.vkDevice(), pipeline, pipelineCallbacks);

			VkAllocationCallbacks pipelineLayoutCallbacks = CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler);
			long[] pipelineLayouts = {
					textScratchPipelineLayout, textTransferPipelineLayout, textIntersectionPipelineLayout,
					blurPipelineLayout1, blurPipelineLayout2, blurPipelineLayoutSample, kimPipelineLayout
			};
			for (long layout : pipelineLayouts) vkDestroyPipelineLayout(boiler.vkDevice(), layout, pipelineLayoutCallbacks);

			VkAllocationCallbacks descriptorLayoutCallbacks = CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler);
			VkbDescriptorSetLayout[] descriptorLayouts = {
					imageDescriptorSetLayout, bufferDescriptorSetLayout, doubleComputeBufferDescriptorLayout,
					textScratchDescriptorLayout1, textTransferDescriptorLayout, textIntersectionDescriptorLayout,
					blurDescriptorLayout1
			};
			for (VkbDescriptorSetLayout layout : descriptorLayouts) {
				if (layout == null) continue;
				vkDestroyDescriptorSetLayout(boiler.vkDevice(), layout.vkDescriptorSetLayout, descriptorLayoutCallbacks);
			}
		}
	}
}
