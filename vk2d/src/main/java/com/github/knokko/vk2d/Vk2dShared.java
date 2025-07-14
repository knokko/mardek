package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dShared {

	public final long pixelatedSampler;
	public final long smoothSampler;

	public final VkbDescriptorSetLayout imageDescriptorSetLayout;
	public final VkbDescriptorSetLayout textDescriptorSetLayout;
	public final VkbDescriptorSetLayout bufferDescriptorSetLayout;

	public final long kimPipelineLayout;

	public Vk2dShared(BoilerInstance boiler) {
		this.pixelatedSampler = boiler.images.createSimpleSampler(
				VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
				VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "Vk2dPixelatedSampler"
		);
		this.smoothSampler = boiler.images.createSimpleSampler(
				VK_FILTER_LINEAR, VK_SAMPLER_MIPMAP_MODE_LINEAR,
				VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER, "Vk2dSmoothSampler"
		);
		try (MemoryStack stack = stackPush()) {
			DescriptorSetLayoutBuilder builder = new DescriptorSetLayoutBuilder(stack, 1);
			builder.set(0, 0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.imageDescriptorSetLayout = builder.build(boiler, "Vk2dImageDescriptorLayout");

			builder = new DescriptorSetLayoutBuilder(stack, 2);
			builder.set(0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
			builder.set(1, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.textDescriptorSetLayout = builder.build(boiler, "Vk2dTextDescriptorLayout");

			builder = new DescriptorSetLayoutBuilder(stack, 1);
			builder.set(
					0, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
					VK_SHADER_STAGE_FRAGMENT_BIT | VK_SHADER_STAGE_VERTEX_BIT
			);
			this.bufferDescriptorSetLayout = builder.build(boiler, "Vk2dBufferDescriptorLayout");
		}
		this.kimPipelineLayout = boiler.pipelines.createLayout(
				null, "Vk2dKimPipelineLayout",
				bufferDescriptorSetLayout.vkDescriptorSetLayout
		);
	}

	public void destroy(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			vkDestroySampler(boiler.vkDevice(), pixelatedSampler, CallbackUserData.SAMPLER.put(stack, boiler));
			vkDestroySampler(boiler.vkDevice(), smoothSampler, CallbackUserData.SAMPLER.put(stack, boiler));
			vkDestroyDescriptorSetLayout(
					boiler.vkDevice(), imageDescriptorSetLayout.vkDescriptorSetLayout,
					CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
			);
			vkDestroyDescriptorSetLayout(
					boiler.vkDevice(), textDescriptorSetLayout.vkDescriptorSetLayout,
					CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
			);
			vkDestroyDescriptorSetLayout(
					boiler.vkDevice(), bufferDescriptorSetLayout.vkDescriptorSetLayout,
					CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
			);
			vkDestroyPipelineLayout(
					boiler.vkDevice(), kimPipelineLayout,
					CallbackUserData.PIPELINE_LAYOUT.put(stack, boiler)
			);
		}
	}
}
