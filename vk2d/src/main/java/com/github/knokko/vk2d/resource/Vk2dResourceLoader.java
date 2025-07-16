package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.VkbBuffer;
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
import java.nio.IntBuffer;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dResourceLoader {

	private final DataInputStream input;
	private MemoryCombiner stagingCombiner;
	private MemoryBlock stagingMemory;
	private VkbImage[] images;
	private boolean[] pixelatedImages;
	private long[] imageDescriptors;
	private MappedVkbBuffer[] imageStagingBuffers;

	private Font[] fonts;

	private VkbBuffer fakeImages;
	private MappedVkbBuffer fakeStagingBuffer;
	private int[] fakeOffsets;
	private int[] fakeWidths;
	private int[] fakeHeights;
	private long fakeImageDescriptor;

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

		int numFakeImages = input.readInt();
		this.fakeOffsets = new int[numFakeImages];
		this.fakeWidths = new int[numFakeImages];
		this.fakeHeights = new int[numFakeImages];

		int fakeOffset = 0;
		for (int index = 0; index < numFakeImages; index++) {
			int intSize = input.readInt();
			fakeWidths[index] = input.readInt();
			fakeHeights[index] = input.readInt();
			this.fakeOffsets[index] = fakeOffset;
			fakeOffset += intSize;
		}

		long fakeImageSize = 4L * fakeOffset;
		if (fakeImageSize > 0L) {
			this.fakeImages = combiner.addBuffer(
					fakeImageSize, boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
					VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, 0.5f
			);
			this.fakeStagingBuffer = stagingCombiner.addMappedBuffer(
					fakeImageSize, 4L, VK_BUFFER_USAGE_TRANSFER_SRC_BIT
			);
		}

		int numFonts = input.readInt();
		this.fonts = new Font[numFonts];
		for (int index = 0; index < numFonts; index++) {
			int numCurves = input.readInt();
			int numGlyphs = input.readInt();
			long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
			int usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT;
			this.fonts[index] = new Font(
					combiner.addBuffer(8L * numCurves, alignment, usage, 0.5f),
					combiner.addMappedBuffer(8L * numCurves, 4, VK_BUFFER_USAGE_TRANSFER_SRC_BIT), numGlyphs
			);
			for (int glyph = 0; glyph < numGlyphs; glyph++) {
				this.fonts[index].firstCurves[glyph] = input.readInt();
				this.fonts[index].numCurves[glyph] = input.readInt();
				this.fonts[index].glyphMinX[glyph] = input.readFloat();
				this.fonts[index].glyphMinY[glyph] = input.readFloat();
				this.fonts[index].glyphMaxX[glyph] = input.readFloat();
				this.fonts[index].glyphMaxY[glyph] = input.readFloat();
			}
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

		if (fakeStagingBuffer != null) {
			IntBuffer fakeData = fakeStagingBuffer.intBuffer();
			while (fakeData.hasRemaining()) fakeData.put(input.readInt());
		}

		for (Font font : fonts) {
			IntBuffer curves = font.curveStagingBuffer.intBuffer();
			while (curves.hasRemaining()) curves.put(input.readInt());
		}
	}

	public void performStaging(CommandRecorder recorder, Vk2dShared shared, DescriptorCombiner descriptors) {
		VkbBuffer[] fontStagingBuffers = new VkbBuffer[fonts.length];
		VkbBuffer[] fontBuffers = new VkbBuffer[fonts.length];
		for (int index = 0; index < fonts.length; index++) {
			fontBuffers[index] = fonts[index].curveBuffer;
			fontStagingBuffers[index] = fonts[index].curveStagingBuffer;
		}

		recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, images);
		recorder.bulkCopyBufferToImage(images, imageStagingBuffers);
		if (fakeImages != null) recorder.copyBuffer(fakeStagingBuffer, fakeImages);
		recorder.bulkCopyBuffers(fontStagingBuffers, fontBuffers);
		recorder.bulkTransitionLayout(
				ResourceUsage.TRANSFER_DEST,
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT), images
		);
		if (fakeImages != null) {
			recorder.bufferBarrier(fakeImages, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
					VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_VERTEX_SHADER_BIT
			));
		}
		recorder.bulkBufferBarrier(ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
				VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
		), fontBuffers);

		if (images.length > 0) {
			// TODO The if (images.length > 0) should be unneeded. Fix in vk-boiler
			this.imageDescriptors = descriptors.addMultiple(shared.imageDescriptorSetLayout, images.length);
		} else {
			this.imageDescriptors = new long[0];
		}
		if (fakeImages != null) descriptors.addSingle(
				shared.bufferDescriptorSetLayout, descriptorSet -> this.fakeImageDescriptor = descriptorSet
		);
		for (Font font : fonts) {
			descriptors.addSingle(shared.bufferDescriptorSetLayout, descriptorSet -> font.descriptorSet = descriptorSet);
		}
	}

	public Vk2dResourceBundle finish(BoilerInstance boiler, Vk2dShared shared) {
		this.stagingMemory.destroy(boiler);
		this.stagingMemory = null;
		this.imageStagingBuffers = null;
		this.fakeStagingBuffer = null;

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

		if (fakeImageDescriptor != 0L) {
			try (MemoryStack stack = stackPush()) {
				DescriptorUpdater updater = new DescriptorUpdater(stack, 1);
				updater.writeStorageBuffer(0, fakeImageDescriptor, 0, fakeImages);
				updater.update(boiler);
			}
		}

		Vk2dFont[] bundleFonts = new Vk2dFont[fonts.length];
		for (int index = 0; index < fonts.length; index++) {
			Font font = fonts[index];
			try (MemoryStack stack = stackPush()) {
				DescriptorUpdater updater = new DescriptorUpdater(stack, 1);
				updater.writeStorageBuffer(0, font.descriptorSet, 0, font.curveBuffer);
				updater.update(boiler);
			}
			bundleFonts[index] = new Vk2dFont(
					font.descriptorSet, font.firstCurves, font.numCurves,
					font.glyphMinX, font.glyphMinY, font.glyphMaxX, font.glyphMaxY
			);
		}

		return new Vk2dResourceBundle(
				imageDescriptors, bundleFonts,
				fakeImageDescriptor, fakeOffsets, fakeWidths, fakeHeights
		);
	}

	private static class Font {

		final VkbBuffer curveBuffer;
		final MappedVkbBuffer curveStagingBuffer;
		final int[] firstCurves, numCurves;
		final float[] glyphMinX, glyphMinY, glyphMaxX, glyphMaxY;

		long descriptorSet;

		Font(VkbBuffer curveBuffer, MappedVkbBuffer curveStagingBuffer, int numGlyphs) {
			this.curveBuffer = curveBuffer;
			this.curveStagingBuffer = curveStagingBuffer;
			this.firstCurves = new int[numGlyphs];
			this.numCurves = new int[numGlyphs];
			this.glyphMinX = new float[numGlyphs];
			this.glyphMinY = new float[numGlyphs];
			this.glyphMaxX = new float[numGlyphs];
			this.glyphMaxY = new float[numGlyphs];
		}
	}
}
