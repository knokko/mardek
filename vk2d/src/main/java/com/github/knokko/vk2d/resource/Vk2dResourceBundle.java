package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.vk2d.text.Vk2dFont;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_blob_destroy;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;

public class Vk2dResourceBundle {

	private final long[] imageDescriptorSets;
	private final int[] imageWidths;
	private final int[] imageHeights;
	public final int numImages;

	private final long[] fontBlobs;
	private final Vk2dFont[] fonts;

	public final long fakeImageDescriptorSet;
	private final int[] fakeImageOffsets;
	private final int[] fakeImageWidths;
	private final int[] fakeImageHeights;
	private final int[] fakeImageData;
	public final int numFakeImages;

	MemoryBlock memory;
	long vkDescriptorPool;

	Vk2dResourceBundle(
			long[] imageDescriptorSets, int[] imageWidths, int[] imageHeights,
			long[] fontBlobs, Vk2dFont[] fonts,
			long fakeImageDescriptorSet, int[] fakeImageOffsets,
			int[] fakeImageWidths, int[] fakeImageHeights, int[] fakeImageData
	) {
		this.imageDescriptorSets = imageDescriptorSets;
		this.numImages = imageDescriptorSets.length;
		this.imageWidths = imageWidths;
		this.imageHeights = imageHeights;
		this.fontBlobs = fontBlobs;
		this.fonts = fonts;

		this.fakeImageDescriptorSet = fakeImageDescriptorSet;
		this.fakeImageOffsets = fakeImageOffsets;
		this.fakeImageWidths = fakeImageWidths;
		this.fakeImageHeights = fakeImageHeights;
		this.fakeImageData = fakeImageData;
		this.numFakeImages = fakeImageHeights.length;
	}

	public long getImageDescriptor(int imageIndex) {
		return imageDescriptorSets[imageIndex];
	}

	public int getImageWidth(int imageIndex) {
		return imageWidths[imageIndex];
	}

	public int getImageHeight(int imageIndex) {
		return imageHeights[imageIndex];
	}

	public Vk2dFont getFont(int fontIndex) {
		return fonts[fontIndex];
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

	public int getFakeImageData(int textureIndex, int dataIndex) {
		if (dataIndex < 0 || dataIndex >= 2) throw new IllegalArgumentException();
		return fakeImageData[2 * textureIndex + dataIndex];
	}

	public void destroy(BoilerInstance boiler) {
		for (var font : fonts) font.destroy();
		for (var blob : fontBlobs) hb_blob_destroy(blob);
		if (memory != null) memory.destroy(boiler);
		if (vkDescriptorPool != VK_NULL_HANDLE) {
			try (MemoryStack stack = MemoryStack.stackPush()) {
				vkDestroyDescriptorPool(
						boiler.vkDevice(), vkDescriptorPool,
						CallbackUserData.DESCRIPTOR_POOL.put(stack, boiler)
				);
			}
		}
	}
}
