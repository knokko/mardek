package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dInstance {

	public final BoilerInstance boiler;
	public final Vk2dConfig config;

	public final long pixelatedSampler;
	public final long smoothSampler;

	public final VkbDescriptorSetLayout imageDescriptorSetLayout;
	public final VkbDescriptorSetLayout bufferDescriptorSetLayout;
	public final VkbDescriptorSetLayout doubleComputeBufferDescriptorLayout;

	public final long colorPipelineLayout;
	public final long singleBufferPipelineLayout;
	public final long kim3PipelineLayout;

	public final VkbDescriptorSetLayout blurDescriptorLayout1;
	public final VkbDescriptorSetLayout sdfGenerateDescriptorLayout;
	public final long sdfGeneratePipeline, sdfGeneratePipelineLayout;
	public final long blurPipelineLayout1, blurPipelineLayout2, blurPipelineLayoutSample;
	public final long blurPipeline1, blurPipeline2;

	@SuppressWarnings("resource")
	public Vk2dInstance(BoilerInstance boiler, Vk2dConfig config) {
		this.boiler = boiler;
		this.config = config;
		try (MemoryStack stack = stackPush()) {
			if (config.image || config.blur) {
				this.pixelatedSampler = boiler.images.createSimpleSampler(
						VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
						VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "Vk2dPixelatedSampler"
				);
			} else {
				this.pixelatedSampler = VK_NULL_HANDLE;
			}

			if (config.shouldCreateColorPipelineLayout()) {
				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8);

				this.colorPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dColorPipelineLayout"
				);
			} else {
				this.colorPipelineLayout = VK_NULL_HANDLE;
			}

			if (config.image || config.simpleText || config.fancyText) {
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

			if (config.shouldCreateBufferPipelineLayout() || config.blur || config.kim3 || config.simpleText || config.fancyText) {
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
				this.singleBufferPipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dSingleBufferPipelineLayout",
						bufferDescriptorSetLayout.vkDescriptorSetLayout
				);
			} else {
				this.singleBufferPipelineLayout = VK_NULL_HANDLE;
			}

			if (config.kim3) {
				VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_VERTEX_BIT, 0, 8);

				assert bufferDescriptorSetLayout != null;
				this.kim3PipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dKim3PipelineLayout",
						bufferDescriptorSetLayout.vkDescriptorSetLayout,
						bufferDescriptorSetLayout.vkDescriptorSetLayout
				);
			} else {
				this.kim3PipelineLayout = VK_NULL_HANDLE;
			}

			if (config.blur) {
				DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 2);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				this.doubleComputeBufferDescriptorLayout = descriptors.build(
						boiler, "Vk2dDoubleComputeBufferDescriptorLayout"
				);
			} else {
				this.doubleComputeBufferDescriptorLayout = null;
			}

			if (config.simpleText || config.fancyText) {
				var descriptors = new DescriptorSetLayoutBuilder(stack, 3);
				descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, VK_SHADER_STAGE_COMPUTE_BIT);
				descriptors.set(2, 2, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, VK_SHADER_STAGE_COMPUTE_BIT);
				this.sdfGenerateDescriptorLayout = descriptors.build(boiler, "Vk2dSdfGenerateDescriptorLayout");

				var pushConstants = VkPushConstantRange.calloc(1, stack);
				pushConstants.get(0).set(VK_SHADER_STAGE_COMPUTE_BIT, 0, 12 * 4);
				this.sdfGeneratePipelineLayout = boiler.pipelines.createLayout(
						pushConstants, "Vk2dSdfGeneratePipelineLayout",
						sdfGenerateDescriptorLayout.vkDescriptorSetLayout
				);

				String shaderPath = "com/github/knokko/vk2d/text/";
				this.sdfGeneratePipeline = boiler.pipelines.createComputePipeline(
						this.sdfGeneratePipelineLayout,
						shaderPath + "generate.comp.spv",
						"Vk2dSdfGeneratePipeline"
				);
			} else {
				this.sdfGenerateDescriptorLayout = null;
				this.sdfGeneratePipeline = VK_NULL_HANDLE;
				this.sdfGeneratePipelineLayout = VK_NULL_HANDLE;
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
				assert doubleComputeBufferDescriptorLayout != null;
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
			long[] pipelines = {
					sdfGeneratePipeline,
					blurPipeline1, blurPipeline2
			};
			for (long pipeline : pipelines) vkDestroyPipeline(boiler.vkDevice(), pipeline, pipelineCallbacks);

			VkAllocationCallbacks pipelineLayoutCallbacks = CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler);
			long[] pipelineLayouts = {
					sdfGeneratePipelineLayout,
					blurPipelineLayout1, blurPipelineLayout2, blurPipelineLayoutSample,
					colorPipelineLayout, singleBufferPipelineLayout, kim3PipelineLayout
			};
			for (long layout : pipelineLayouts) vkDestroyPipelineLayout(boiler.vkDevice(), layout, pipelineLayoutCallbacks);

			VkAllocationCallbacks descriptorLayoutCallbacks = CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler);
			VkbDescriptorSetLayout[] descriptorLayouts = {
					imageDescriptorSetLayout, bufferDescriptorSetLayout, doubleComputeBufferDescriptorLayout,
					sdfGenerateDescriptorLayout,
					blurDescriptorLayout1
			};
			for (VkbDescriptorSetLayout layout : descriptorLayouts) {
				if (layout == null) continue;
				vkDestroyDescriptorSetLayout(boiler.vkDevice(), layout.vkDescriptorSetLayout, descriptorLayoutCallbacks);
			}
		}
	}
}
