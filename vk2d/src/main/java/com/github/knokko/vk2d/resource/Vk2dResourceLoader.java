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
import org.lwjgl.util.zstd.ZSTDInBuffer;
import org.lwjgl.util.zstd.ZSTDOutBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import static com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple;
import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static java.lang.Math.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.util.zstd.Zstd.*;
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
		loader.claimDescriptors(descriptorCombiner);
		long descriptorPool = descriptorCombiner.build("Vk2dResourceLoader simple");
		loader.performStaging(null);

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
	private final DataReader input;
	private final Runnable destroyInput;

	private MemoryCombiner stagingCombiner;
	private MemoryBlock stagingMemory;
	private VkbImage[] images;
	private boolean[] pixelatedImages;
	private long[] imageDescriptors;
	private StagingJob[] stagingJobs;
	private MappedVkbBuffer sharedStagingBuffer;

	private Bc4Compressor bc4Compressor;
	private long[] fontBlobs;
	private Vk2dFont[] fonts;

	private VkbBuffer fakeImages;
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
		var allBytes = decompress(input);
		this.input = numBytes -> {
			ByteBuffer result = allBytes.slice(allBytes.position(), numBytes);
			allBytes.position(allBytes.position() + numBytes);
			return result;
		};
		this.destroyInput = () -> {
			memFree(input);
			memFree(allBytes);
		};
	}

	@SuppressWarnings("resource")
	public Vk2dResourceLoader(Vk2dInstance instance, Path inputFile) {
		this.instance = instance;

		long stream = ZSTD_createDStream();
		var streamedData = new ByteBuffer[] { memCalloc(1_000_000) };
		var compressedData = memCalloc(5_000_000);
		compressedData.limit(0);

		var streamInput = ZSTDInBuffer.calloc();
		streamInput.set(compressedData, 0);
		var streamOutput = ZSTDOutBuffer.calloc();
		FileChannel channel;
		try {
			channel = FileChannel.open(inputFile, StandardOpenOption.READ);
		} catch (IOException channelFailed) {
			throw new RuntimeException("Failed to open channel for " + inputFile, channelFailed);
		}

		this.input = numBytes -> {
			int oldCapacity = streamedData[0].capacity();
			if (numBytes > oldCapacity) {
				streamedData[0] = memRealloc(streamedData[0], max(numBytes, 2 * oldCapacity));
			}

			streamedData[0].position(0);
			streamedData[0].limit(numBytes);
			streamOutput.set(streamedData[0], 0L);

			while (streamOutput.pos() < numBytes) {

				long decompressResult = ZSTD_decompressStream(stream, streamOutput, streamInput);
				if (ZSTD_isError(decompressResult)) {
					throw new RuntimeException("ZSTD_decompressStream failed with " + decompressResult +
							" (" + ZSTD_getErrorName(decompressResult) + ") for " + inputFile
					);
				}

				if (streamOutput.pos() == numBytes) break;

				compressedData.position(0);
				compressedData.limit(compressedData.capacity());
				try {
					channel.read(compressedData);
				} catch (IOException channelFailed) {
					throw new RuntimeException(channelFailed);
				}
				compressedData.flip();

				streamInput.set(compressedData, 0);
			}

			if (streamOutput.pos() != numBytes) {
				throw new RuntimeException("Not all bytes were read: " + streamOutput.pos() + " / " + numBytes);
			}
			streamedData[0].order(ByteOrder.BIG_ENDIAN);
			return streamedData[0];
		};
		this.destroyInput = () -> {
			ZSTD_freeDStream(stream);
			memFree(streamedData[0]);
			memFree(compressedData);
			streamInput.free();
			streamOutput.free();
			try {
				channel.close();
			} catch (IOException channelFailed) {
				throw new RuntimeException("Failed to close channel for " + inputFile, channelFailed);
			}
		};
	}

	public void claimMemory(MemoryCombiner combiner) throws IOException {
		claimMemory(combiner, 20_000_000L);
	}

	public void claimMemory(MemoryCombiner combiner, long desiredStagingBufferSize) {
		this.stagingCombiner = new MemoryCombiner(instance.boiler, "Vk2dStaging");

		int numImages = input.readData(4).getInt();
		this.images = new VkbImage[numImages];
		this.pixelatedImages = new boolean[numImages];
		var imageStagingJobs = new StagingJob[numImages];

		var imageInfoBuffer = input.readData(4 + 10 * numImages);
		for (int index = 0; index < numImages; index++) {
			int width = imageInfoBuffer.getInt();
			int height = imageInfoBuffer.getInt();
			Vk2dImageCompression compression = Vk2dImageCompression.values()[imageInfoBuffer.get()];
			this.pixelatedImages[index] = imageInfoBuffer.get() == 1;
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
			var finalImage = images[index];
			imageStagingJobs[index] = new StagingJob(
					size, compression.alignment,
					destination -> destination.put(destination.position(), input.readData(size), 0, size),
					(recorder, stagingBuffer) -> recorder.copyBufferToImage(finalImage, stagingBuffer)
			);
		}

		int numFakeImages = imageInfoBuffer.getInt();
		var fakeImageInfoBuffer = input.readData(4 + 12 * numFakeImages);
		this.fakeOffsets = new int[numFakeImages];
		this.fakeWidths = new int[numFakeImages];
		this.fakeHeights = new int[numFakeImages];
		this.fakeData = new int[2 * numFakeImages];

		int fakeOffset = 0;
		for (int index = 0; index < numFakeImages; index++) {
			int intSize = fakeImageInfoBuffer.getInt();
			fakeWidths[index] = fakeImageInfoBuffer.getInt();
			fakeHeights[index] = fakeImageInfoBuffer.getInt();
			this.fakeOffsets[index] = fakeOffset;
			fakeOffset += intSize;
		}

		StagingJob fakeStagingJob = null;
		int fakeImageSize = 4 * fakeOffset;
		if (fakeImageSize > 0) {
			this.fakeImages = combiner.addBuffer(
					fakeImageSize, instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment(),
					VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, 0.5f
			);
			fakeStagingJob = new StagingJob(
					fakeImageSize, 4,
					destination -> {
						IntBuffer fakeData = destination.asIntBuffer();
						var getFakeData = input.readData(fakeImageSize);
						for (int counter = 0; counter < fakeImageSize / 4; counter++) fakeData.put(getFakeData.getInt());
						for (int textureIndex = 0; textureIndex < fakeOffsets.length; textureIndex++) {
							int textureOffset = fakeOffsets[textureIndex];
							this.fakeData[2 * textureIndex] = fakeData.get(textureOffset);
							this.fakeData[2 * textureIndex + 1] = fakeData.get(textureOffset + 1);
						}
					},
					(recorder, stagingBuffer) -> recorder.copyBuffer(stagingBuffer, fakeImages)
			);
		}

		int numFontBlobs = fakeImageInfoBuffer.getInt();
		this.fontBlobs = new long[numFontBlobs];
		var fonts = new ArrayList<Vk2dFont>(numFontBlobs);

		for (int blobIndex = 0; blobIndex < numFontBlobs; blobIndex++) {
			int numTtfBytes = input.readData(4).getInt();
			var blobData = input.readData(4 + numTtfBytes);

			var hbBlob = assertHbSuccess(hb_blob_create(
					blobData.slice(0, numTtfBytes), HB_MEMORY_MODE_DUPLICATE, 0L, null
			), "blob_create");
			this.fontBlobs[blobIndex] = hbBlob;

			int numFonts = blobData.getInt(numTtfBytes);
			for (int fontIndex = 0; fontIndex < numFonts; fontIndex++) {
				var fontInfo = input.readData(8);
				int faceIndex = fontInfo.getInt();
				long hbFace = assertHbSuccess(hb_face_create(hbBlob, faceIndex), "face_create");

				var font = new Vk2dFont(hbFace, instance, combiner);
				int numAtlases = fontInfo.getInt();
				for (int atlasCounter = 0; atlasCounter < numAtlases; atlasCounter++) {
					int[] supportedGlyphs = null;
					int numSupportedGlyphs = input.readData(4).getInt();
					var atlasData = input.readData(25 + 4 * max(0, numSupportedGlyphs));
					if (numSupportedGlyphs >= 0) {
						supportedGlyphs = new int[numSupportedGlyphs];
						for (int glyphIndex = 0; glyphIndex < numSupportedGlyphs; glyphIndex++) {
							supportedGlyphs[glyphIndex] = atlasData.getInt();
						}
					}

					int bitsPerPixel = atlasData.get();
					font.addAtlas(
							instance, bitsPerPixel, atlasData.getFloat(), atlasData.getFloat(), combiner,
							stagingCombiner, atlasData.getFloat(), atlasData.getFloat(),
							atlasData.getFloat(), atlasData.getFloat(), supportedGlyphs
					);
					if (bitsPerPixel == 4 && bc4Compressor == null) {
						this.bc4Compressor = new Bc4Compressor(instance.boiler);
					}
				}

				fonts.add(font);
			}
		}

		this.fonts = fonts.toArray(Vk2dFont[]::new);

		int numStagingJobs = imageStagingJobs.length + fonts.size();
		if (fakeStagingJob != null) numStagingJobs += 1;
		this.stagingJobs = new StagingJob[numStagingJobs];
		System.arraycopy(imageStagingJobs, 0, stagingJobs, 0, imageStagingJobs.length);
		if (fakeStagingJob != null) stagingJobs[images.length] = fakeStagingJob;
		for (int fontIndex = 0; fontIndex < fonts.size(); fontIndex++) {
			var font = this.fonts[fontIndex];
			int baseIndex = images.length;
			if (fakeStagingJob != null) baseIndex += 1;
			this.stagingJobs[fontIndex + baseIndex] = new StagingJob(
					font.determineStagingBufferSize(), 4,
					font::fillStagingBuffer, font::performStagingCopy
			);
		}

		long minimumStagingBufferSize = 0L;
		long totalStagingBufferSize = 0L;
		long stagingBufferAlignment = 1L;
		for (var buffer : stagingJobs) {
			minimumStagingBufferSize = max(minimumStagingBufferSize, buffer.requiredSize);
			totalStagingBufferSize = nextMultipleOf(totalStagingBufferSize, buffer.alignment);
			totalStagingBufferSize += buffer.requiredSize;
			stagingBufferAlignment = leastCommonMultiple(stagingBufferAlignment, buffer.alignment);
		}
		long stagingBufferSize = max(minimumStagingBufferSize, min(desiredStagingBufferSize, totalStagingBufferSize));
		this.sharedStagingBuffer = stagingCombiner.addMappedDeviceLocalBuffer(
				stagingBufferSize, stagingBufferAlignment, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, 0.25f
		);
	}

	public void claimDescriptors(DescriptorCombiner descriptors) {
		for (var font : fonts) font.claimDescriptors(instance, descriptors, bc4Compressor);

		if (instance.imageDescriptorSetLayout != null) {
			this.imageDescriptors = descriptors.addMultiple(instance.imageDescriptorSetLayout, images.length);
		} else {
			this.imageDescriptors = new long[0];
		}

		if (fakeImages != null && instance.bufferDescriptorSetLayout != null) descriptors.addSingle(
				instance.bufferDescriptorSetLayout, descriptorSet -> this.fakeImageDescriptor = descriptorSet
		);
	}

	public void performStaging(SingleTimeCommands commands) {
		long longTimeout = 30_000_000_000L;
		boolean shouldDestroyCommands = commands == null;
		if (commands == null) commands = new SingleTimeCommands(instance.boiler);

		this.stagingMemory = stagingCombiner.build(false);
		this.stagingCombiner = null;

		int startJobIndex = 0;
		while (startJobIndex < stagingJobs.length) {
			int endJobIndex = startJobIndex - 1;
			var stagingBytes = sharedStagingBuffer.byteBuffer();
			while (endJobIndex + 1 < stagingJobs.length) {
				var nextJob = stagingJobs[endJobIndex + 1];

				int nextStartPosition = nextMultipleOf(stagingBytes.position(), toIntExact(nextJob.alignment));
				int nextEndPosition = nextStartPosition + nextJob.requiredSize;
				if (nextEndPosition > stagingBytes.capacity()) break;

				endJobIndex += 1;
				stagingBytes.position(nextStartPosition);
				nextJob.putData().put(stagingBytes);
				stagingBytes.position(nextEndPosition);
			}
			stagingBytes.position(0);
			final int finalStartJobIndex = startJobIndex;
			final int finalEndJobIndex = endJobIndex;

			boolean isFirst = startJobIndex == 0;
			boolean isLast = endJobIndex == stagingJobs.length - 1;
			commands.submit("Vk2dResourceLoader", recorder -> {
				if (isFirst) recorder.bulkTransitionLayout(null, ResourceUsage.TRANSFER_DEST, images);
				for (int jobIndex = finalStartJobIndex; jobIndex <= finalEndJobIndex; jobIndex++) {
					var job = stagingJobs[jobIndex];
					stagingBytes.position(nextMultipleOf(stagingBytes.position(), job.alignment));
					var bufferSlice = sharedStagingBuffer.child(stagingBytes.position(), job.requiredSize);
					stagingBytes.position(stagingBytes.position() + job.requiredSize);
					job.transferData().transfer(recorder, bufferSlice);
				}
				if (isLast) {
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
			}).awaitCompletion(longTimeout);
			startJobIndex = endJobIndex + 1;
		}

		if (shouldDestroyCommands) commands.destroy(longTimeout);
		destroyInput.run();
	}

	public Vk2dResourceBundle finish() {
		if (bc4Compressor != null) {
			this.bc4Compressor.destroy();
			this.bc4Compressor = null;
		}

		this.stagingMemory.destroy(instance.boiler);
		this.stagingMemory = null;
		this.stagingJobs = null;

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
		return new Vk2dResourceBundle(
				imageDescriptors, imageWidths, imageHeights,
				fontBlobs, fonts,
				fakeImageDescriptor, fakeOffsets,
				fakeWidths, fakeHeights, fakeData
		);
	}

	private record StagingJob(
			int requiredSize, int alignment, PutStagingData putData, TransferStagingData transferData
	) {}

	@FunctionalInterface
	private interface PutStagingData {

		void put(ByteBuffer destination);
	}

	@FunctionalInterface
	private interface TransferStagingData {

		void transfer(CommandRecorder recorder, MappedVkbBuffer buffer);
	}

	@FunctionalInterface
	private interface DataReader {

		ByteBuffer readData(int numBytes);
	}
}
