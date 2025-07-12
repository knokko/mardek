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

	Vk2dResourceBundle(long vkDescriptorPool, long[] imageDescriptorSets) {
		this.vkDescriptorPool = vkDescriptorPool;
		this.imageDescriptorSets = imageDescriptorSets;
		this.numImages = imageDescriptorSets.length;
	}

	public long getImageDescriptor(int imageIndex) {
		return imageDescriptorSets[imageIndex];
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
