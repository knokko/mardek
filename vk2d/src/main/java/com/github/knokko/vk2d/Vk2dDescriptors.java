package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorSetLayoutBuilder;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.LongConsumer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dDescriptors {

	public final long pixelatedSampler, smoothSampler;
	public final VkbDescriptorSetLayout imageLayout;

	private DescriptorCombiner combiner;
	private long descriptorPool;

	private final Collection<SingleImageEntry> imagesToPopulate = new ArrayList<>();
	private final Collection<MultiImageEntry> imageArraysToPopulate = new ArrayList<>();

	public Vk2dDescriptors(BoilerInstance boiler) {
		this.pixelatedSampler = boiler.images.createSimpleSampler(
				VK_FILTER_NEAREST, VK_FILTER_NEAREST,
				VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER,
				"Vk2dPixelatedSampler"
		);
		this.smoothSampler = boiler.images.createSimpleSampler(
				VK_FILTER_LINEAR, VK_SAMPLER_MIPMAP_MODE_NEAREST,
				VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER,
				"Vk2dSmoothSampler"
		);
		this.combiner = new DescriptorCombiner(boiler);

		try (MemoryStack stack = stackPush()) {
			DescriptorSetLayoutBuilder descriptors = new DescriptorSetLayoutBuilder(stack, 1);
			descriptors.set(0, 0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT);
			this.imageLayout = descriptors.build(boiler, "Vk2dImageDescriptorLayout");
		}
	}

	public void addSingleImage(VkbImage image, boolean pixelated, LongConsumer callback) {
		combiner.addSingle(imageLayout, descriptorSet -> {
			imagesToPopulate.add(new SingleImageEntry(descriptorSet, image, pixelated));
			callback.accept(descriptorSet);
		});
	}

	public long[] addImageArray(VkbImage[] images, boolean pixelated) {
		long[] descriptorSets = combiner.addMultiple(imageLayout, images.length);
		imageArraysToPopulate.add(new MultiImageEntry(descriptorSets, images, pixelated));
		return descriptorSets;
	}

	public void finish(BoilerInstance boiler) {
		descriptorPool = combiner.build("Vk2dDescriptors");
		combiner = null;

		// TODO Add bulk descriptor update support to vk-boiler
		for (SingleImageEntry single : imagesToPopulate) {
			try (MemoryStack stack = stackPush()) {
				DescriptorUpdater updater = new DescriptorUpdater(stack, 1);
				long sampler = single.pixelated ? pixelatedSampler : smoothSampler;
				updater.writeImage(0, single.descriptorSet, 0, single.image.vkImageView, sampler);
				updater.update(boiler);
			}
		}

		for (MultiImageEntry multiple : imageArraysToPopulate) {
			for (int index = 0; index < multiple.descriptorSets.length; index++) {
				long sampler = multiple.pixelated ? pixelatedSampler : smoothSampler;
				try (MemoryStack stack = stackPush()) {
					DescriptorUpdater updater = new DescriptorUpdater(stack, 1);
					updater.writeImage(
							0, multiple.descriptorSets[index], 0,
							multiple.images[index].vkImageView, sampler
					);
					updater.update(boiler);
				}
			}
		}
	}

	public void destroy(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			vkDestroyDescriptorSetLayout(
					boiler.vkDevice(), imageLayout.vkDescriptorSetLayout,
					CallbackUserData.DESCRIPTOR_SET_LAYOUT.put(stack, boiler)
			);
			vkDestroyDescriptorPool(
					boiler.vkDevice(), descriptorPool,
					CallbackUserData.DESCRIPTOR_POOL.put(stack, boiler)
			);
			vkDestroySampler(boiler.vkDevice(), pixelatedSampler, CallbackUserData.SAMPLER.put(stack, boiler));
			vkDestroySampler(boiler.vkDevice(), smoothSampler, CallbackUserData.SAMPLER.put(stack, boiler));
		}
	}

	private record SingleImageEntry(long descriptorSet, VkbImage image, boolean pixelated) {}

	private record MultiImageEntry(long[] descriptorSets, VkbImage[] images, boolean pixelated) {}
}
