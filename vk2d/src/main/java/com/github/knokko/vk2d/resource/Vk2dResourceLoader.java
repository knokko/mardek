package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.BulkDescriptorUpdater;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.images.VkbImage;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.compressor.Bc4Compressor;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.text.Vk2dFont;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.util.zstd.Zstd.ZSTD_decompress;
import static org.lwjgl.util.zstd.Zstd.ZSTD_getFrameContentSize;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dResourceLoader {

	public static Vk2dResourceBundle loadSimpleAndPotentiallyInefficient(
			Vk2dInstance instance, InputStream input
	) throws IOException {
		var loader = new Vk2dResourceLoader(instance, input);
		var memoryCombiner = new MemoryCombiner(instance.boiler, "Vk2dResourceLoader simple");
		loader.claimMemory(memoryCombiner);
		var memoryBlock = memoryCombiner.build(instance.boiler.vmaAllocator() != VK_NULL_HANDLE);
		var descriptorCombiner = new DescriptorCombiner(instance.boiler);
		loader.prepareStaging(descriptorCombiner);
		long descriptorPool = descriptorCombiner.build("Vk2dResourceLoader simple");
		SingleTimeCommands.submit(
				instance.boiler, "Vk2dResourceLoader simple", loader::performStaging
		).destroy();

		var bundle = loader.finish();
		bundle.memory = memoryBlock;
		bundle.vkDescriptorPool = descriptorPool;
		return bundle;
	}

	private static ByteBuffer decompress(ByteBuffer compressed) {
		long expectedUncompressedSize = ZSTD_getFrameContentSize(compressed);
		var uncompressed = memCalloc(Math.toIntExact(expectedUncompressedSize)).order(ByteOrder.BIG_ENDIAN);
		long actualUncompressedSize = ZSTD_decompress(uncompressed, compressed);
		if (expectedUncompressedSize != actualUncompressedSize) throw new RuntimeException("Size mismatch");
		return uncompressed;
	}

	private static ByteBuffer toByteBuffer(byte[] byteArray) {
		return memCalloc(byteArray.length).put(0, byteArray);
	}

	private final Vk2dInstance instance;
	private final ByteBuffer input;

	private MemoryCombiner stagingCombiner;
	private MemoryBlock stagingMemory;
	private VkbImage[] images;
	private boolean[] pixelatedImages;
	private long[] imageDescriptors;
	private MappedVkbBuffer[] imageStagingBuffers;

	private Bc4Compressor bc4Compressor;
	private long[] fontBlobs;
	private Vk2dFont[] fonts;

	private VkbBuffer fakeImages;
	private MappedVkbBuffer fakeStagingBuffer;
	private int[] fakeOffsets;
	private int[] fakeWidths;
	private int[] fakeHeights;
	private int[] fakeData;
	private long fakeImageDescriptor;

	public Vk2dResourceLoader(Vk2dInstance instance, InputStream inputStream) throws IOException {
		this(instance, toByteBuffer(inputStream.readAllBytes()));
	}

	public Vk2dResourceLoader(Vk2dInstance instance, ByteBuffer input) {
		this.instance = instance;
		this.input = decompress(input);
		memFree(input);
	}

	public Vk2dResourceLoader(Vk2dInstance instance, Path inputFile) {
		this.instance = instance;
		try (var channel = FileChannel.open(inputFile, StandardOpenOption.READ)) {
			var compressed = memCalloc(Math.toIntExact(channel.size()));
			while (compressed.position() < compressed.limit()) {
				channel.read(compressed);
			}
			compressed.position(0);
			this.input = decompress(compressed);
			memFree(compressed);
		} catch (IOException failed) {
			throw new RuntimeException(failed);
		}
	}

	public void claimMemory(MemoryCombiner combiner) throws IOException {
		this.stagingCombiner = new MemoryCombiner(instance.boiler, "Vk2dStaging");

		int numImages = input.getInt();
		this.images = new VkbImage[numImages];
		this.pixelatedImages = new boolean[numImages];
		this.imageStagingBuffers = new MappedVkbBuffer[numImages];

		for (int index = 0; index < numImages; index++) {
			int width = input.getInt();
			int height = input.getInt();
			Vk2dImageCompression compression = Vk2dImageCompression.values()[input.get()];
			this.pixelatedImages[index] = input.get() == 1;
			this.images[index] = combiner.addImage(new ImageBuilder(
					"Image" + index, width, height
			).texture().format(compression.format), 0.5f);

			int size = switch (compression) {
				case NONE: yield 4 * width * height;
				case BC1:
				case BC4:
				case BC7:
					int paddedWidth = nextMultipleOf(width, 4);
					int paddedHeight = nextMultipleOf(height, 4);
					if (compression == Vk2dImageCompression.BC7) yield paddedWidth * paddedHeight;
					else yield paddedWidth * paddedHeight / 2;
			};
			this.imageStagingBuffers[index] = stagingCombiner.addMappedDeviceLocalBuffer(
					size, compression.alignment, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, 0.25f
			);
		}

		int numFakeImages = input.getInt();
		this.fakeOffsets = new int[numFakeImages];
		this.fakeWidths = new int[numFakeImages];
		this.fakeHeights = new int[numFakeImages];
		this.fakeData = new int[2 * numFakeImages];

		int fakeOffset = 0;
		for (int index = 0; index < numFakeImages; index++) {
			int intSize = input.getInt();
			fakeWidths[index] = input.getInt();
			fakeHeights[index] = input.getInt();
			this.fakeOffsets[index] = fakeOffset;
			fakeOffset += intSize;
		}

		long fakeImageSize = 4L * fakeOffset;
		if (fakeImageSize > 0L) {
			this.fakeImages = combiner.addBuffer(
					fakeImageSize, instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
					VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, 0.5f
			);
			this.fakeStagingBuffer = stagingCombiner.addMappedBuffer(
					fakeImageSize, 4L, VK_BUFFER_USAGE_TRANSFER_SRC_BIT
			);
		}

		int numFontBlobs = input.getInt();
		this.fontBlobs = new long[numFontBlobs];
		var fonts = new ArrayList<Vk2dFont>(numFontBlobs);

		for (int blobIndex = 0; blobIndex < numFontBlobs; blobIndex++) {
			int numTtfBytes = input.getInt();

			var hbBlob = assertHbSuccess(hb_blob_create(
					input.slice(input.position(), numTtfBytes), HB_MEMORY_MODE_DUPLICATE, 0L, null
			), "blob_create");
			input.position(input.position() + numTtfBytes);
			this.fontBlobs[blobIndex] = hbBlob;

			int numFonts = input.getInt();
			for (int fontIndex = 0; fontIndex < numFonts; fontIndex++) {
				int faceIndex = input.getInt();
				long hbFace = assertHbSuccess(hb_face_create(hbBlob, faceIndex), "face_create");

				var font = new Vk2dFont(hbFace, instance, combiner, stagingCombiner);
				int numAtlases = input.getInt();
				for (int atlasCounter = 0; atlasCounter < numAtlases; atlasCounter++) {
					int[] supportedGlyphs = null;
					int numSupportedGlyphs = input.getInt();
					if (numSupportedGlyphs >= 0) {
						supportedGlyphs = new int[numSupportedGlyphs];
						for (int glyphIndex = 0; glyphIndex < numSupportedGlyphs; glyphIndex++) {
							supportedGlyphs[glyphIndex] = input.getInt();
						}
					}

					int bitsPerPixel = input.get();
					font.addAtlas(
							instance, bitsPerPixel, input.getFloat(), input.getFloat(), combiner, stagingCombiner,
							input.getFloat(), input.getFloat(),
							input.getFloat(), input.getFloat(), supportedGlyphs
					);
					if (bitsPerPixel == 4 && bc4Compressor == null) {
						this.bc4Compressor = new Bc4Compressor(instance.boiler);
					}
				}

				fonts.add(font);
			}
		}

		this.fonts = fonts.toArray(Vk2dFont[]::new);
	}

	public void prepareStaging(DescriptorCombiner descriptors) throws IOException {
		this.stagingMemory = stagingCombiner.build(false);
		this.stagingCombiner = null;
		for (MappedVkbBuffer buffer : imageStagingBuffers) {
			buffer.byteBuffer().put(0, input, input.position(), Math.toIntExact(buffer.size));
			input.position(input.position() + Math.toIntExact(buffer.size));
		}

		if (fakeStagingBuffer != null) {
			IntBuffer fakeData = fakeStagingBuffer.intBuffer();
			while (fakeData.hasRemaining()) fakeData.put(input.getInt());
			for (int textureIndex = 0; textureIndex < fakeOffsets.length; textureIndex++) {
				int textureOffset = fakeOffsets[textureIndex];
				this.fakeData[2 * textureIndex] = fakeData.get(textureOffset);
				this.fakeData[2 * textureIndex + 1] = fakeData.get(textureOffset + 1);
			}
		}

		for (var font : fonts) font.prepareStaging(instance, descriptors, bc4Compressor);

		if (instance.imageDescriptorSetLayout != null) {
			this.imageDescriptors = descriptors.addMultiple(instance.imageDescriptorSetLayout, images.length);
		} else {
			this.imageDescriptors = new long[0];
		}

		if (fakeImages != null && instance.bufferDescriptorSetLayout != null) descriptors.addSingle(
				instance.bufferDescriptorSetLayout, descriptorSet -> this.fakeImageDescriptor = descriptorSet
		);
	}

	public void performStaging(CommandRecorder recorder) {
		recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, images);
		recorder.bulkCopyBufferToImage(images, imageStagingBuffers);
		if (fakeImages != null) recorder.copyBuffer(fakeStagingBuffer, fakeImages);
		recorder.bulkTransitionLayout(
				ResourceUsage.TRANSFER_DEST,
				ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT), images
		);
		if (fakeImages != null) {
			recorder.bufferBarrier(fakeImages, ResourceUsage.TRANSFER_DEST, ResourceUsage.shaderRead(
					VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_VERTEX_SHADER_BIT
			));
		}

		if (fonts.length > 0) Vk2dFont.generateAtlases(instance, recorder, bc4Compressor, fonts);
	}

	public Vk2dResourceBundle finish() {
		if (bc4Compressor != null) {
			this.bc4Compressor.destroy();
			this.bc4Compressor = null;
		}

		this.stagingMemory.destroy(instance.boiler);
		this.stagingMemory = null;
		this.imageStagingBuffers = null;
		this.fakeStagingBuffer = null;

		BulkDescriptorUpdater updater = new BulkDescriptorUpdater(
				instance.boiler, null,
				2 + images.length, 2, images.length
		);
		for (int index = 0; index < images.length; index++) {
			if (imageDescriptors.length == 0) break;
			long sampler = pixelatedImages[index] ? instance.pixelatedSampler : instance.smoothSampler;
			updater.writeImage(imageDescriptors[index], 0, images[index].vkImageView, sampler);
		}

		if (fakeImageDescriptor != 0L) {
			updater.writeStorageBuffer(fakeImageDescriptor, 0, fakeImages);
		}

		updater.finish();

		int[] imageWidths = new int[images.length];
		int[] imageHeights = new int[images.length];
		for (int index = 0; index < images.length; index++) {
			imageWidths[index] = images[index].width;
			imageHeights[index] = images[index].height;
		}
		memFree(input);
		return new Vk2dResourceBundle(
				imageDescriptors, imageWidths, imageHeights,
				fontBlobs, fonts,
				fakeImageDescriptor, fakeOffsets,
				fakeWidths, fakeHeights, fakeData
		);
	}
}
