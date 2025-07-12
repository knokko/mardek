package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dShared;
import org.lwjgl.system.MemoryStack;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

public class Vk2dResourceLoader {

	private final DataInputStream input;
	private MemoryCombiner stagingCombiner;
	private MemoryBlock stagingMemory;
	private VkbImage[] images;
	private boolean[] pixelatedImages;
	private long[] imageDescriptors;
	private MappedVkbBuffer[] imageStagingBuffers;
	private long descriptorPool;

	public Vk2dResourceLoader(InputStream rawInput) {
		this.input = new DataInputStream(rawInput);
	}

	public void claimMemory(BoilerInstance boiler, MemoryCombiner combiner) throws IOException {
		this.stagingCombiner = new MemoryCombiner(boiler, "Vk2dStaging");

		int numImages = input.readInt();
		this.images = new VkbImage[numImages];
		this.pixelatedImages = new boolean[numImages];
		this.imageStagingBuffers = new MappedVkbBuffer[numImages];

		for (int index = 0; index < numImages; index++) {
			int width = input.readInt();
			int height = input.readInt();
			Vk2dImageCompression compression = Vk2dImageCompression.values()[input.readByte()];
			this.pixelatedImages[index] = input.readByte() == 1;
			this.images[index] = combiner.addImage(new ImageBuilder(
					"Image" + index, width, height
			).texture().format(compression.format), 0.5f);

			int size;
			if (compression == Vk2dImageCompression.NONE) size = 4 * width * height;
			else if (compression == Vk2dImageCompression.BC1 || compression == Vk2dImageCompression.BC7) {
				int numBlocks = nextMultipleOf(width, 4) * nextMultipleOf(height, 4) / 16;
				if (compression == Vk2dImageCompression.BC1) size = 8 * numBlocks;
				else size = 16 * numBlocks;
			} else throw new UnsupportedOperationException("Unexpected compression " + compression);
			this.imageStagingBuffers[index] = stagingCombiner.addMappedBuffer(
					size, compression.alignment, VK_BUFFER_USAGE_TRANSFER_SRC_BIT
			);
		}
	}

	public void prepareStaging() throws IOException {
		this.stagingMemory = stagingCombiner.build(false);
		this.stagingCombiner = null;
		for (MappedVkbBuffer buffer : imageStagingBuffers) {
			// TODO Try out channels instead: Channels.newChannel(input)?
			byte[] bytes = new byte[Math.toIntExact(buffer.size)];
			input.readFully(bytes);
			buffer.byteBuffer().put(bytes);
		}
	}

	public void performStaging(BoilerInstance boiler, CommandRecorder recorder, Vk2dShared shared) {
		recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, images);
		recorder.bulkCopyBufferToImage(images, imageStagingBuffers);
		recorder.bulkTransitionLayout(
				ResourceUsage.TRANSFER_DEST,
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT), images
		);

		DescriptorCombiner descriptors = new DescriptorCombiner(boiler);
		this.imageDescriptors = descriptors.addMultiple(shared.imageDescriptorSetLayout, images.length);
		this.descriptorPool = descriptors.build("Vk2dDescriptorPool");
	}

	public Vk2dResourceBundle finish(BoilerInstance boiler, Vk2dShared shared) {
		this.stagingMemory.destroy(boiler);
		this.stagingMemory = null;
		this.imageStagingBuffers = null;

		for (int index = 0; index < images.length; index++) {
			try (MemoryStack stack = stackPush()) {
				// TODO Add bulk descriptor update to vk-boiler
				DescriptorUpdater updater = new DescriptorUpdater(stack, 1);
				long sampler = pixelatedImages[index] ? shared.pixelatedSampler : shared.smoothSampler;
				updater.writeImage(
						0, imageDescriptors[index], 0,
						images[index].vkImageView, sampler
				);
				updater.update(boiler);
			}
		}

		return new Vk2dResourceBundle(descriptorPool, imageDescriptors);
	}
}
