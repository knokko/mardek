package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;

public class Vk2dResourceBundle {

	private final long vkDescriptorPool;

	private final long[] imageDescriptorSets;
	public final int numImages;

	public final long fakeImageDescriptorSet;
	private final int[] fakeImageOffsets;
	private final int[] fakeImageWidths;
	private final int[] fakeImageHeights;
	public final int numFakeImages;

	Vk2dResourceBundle(
			long vkDescriptorPool, long[] imageDescriptorSets,
			long fakeImageDescriptorSet, int[] fakeImageOffsets, int[] fakeImageWidths, int[] fakeImageHeights
	) {
		this.vkDescriptorPool = vkDescriptorPool;
		this.imageDescriptorSets = imageDescriptorSets;
		this.numImages = imageDescriptorSets.length;

		this.fakeImageDescriptorSet = fakeImageDescriptorSet;
		this.fakeImageOffsets = fakeImageOffsets;
		this.fakeImageWidths = fakeImageWidths;
		this.fakeImageHeights = fakeImageHeights;
		this.numFakeImages = fakeImageHeights.length;
	}

	public long getImageDescriptor(int imageIndex) {
		return imageDescriptorSets[imageIndex];
	}

	public int getFakeImageOffset(int index) {
		return fakeImageOffsets[index];
	}

	public int getFakeImageWidth(int index) {
		return fakeImageWidths[index];
	}

	public int getFakeImageHeight(int index) {
		return fakeImageHeights[index];
	}

	public void destroy(BoilerInstance boiler) {
		try (MemoryStack stack = stackPush()) {
			vkDestroyDescriptorPool(
					boiler.vkDevice(), vkDescriptorPool,
					CallbackUserData.DESCRIPTOR_POOL.put(stack, boiler)
			);
		}
	}
}
