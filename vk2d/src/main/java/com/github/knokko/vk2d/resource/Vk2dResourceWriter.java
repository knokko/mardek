package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.utilities.ImageCoding;
import com.github.knokko.compressor.*;
import com.github.knokko.vk2d.Vk2dInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static java.lang.Math.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.util.zstd.Zstd.ZSTD_compress;
import static org.lwjgl.util.zstd.Zstd.ZSTD_compressBound;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dResourceWriter {

	private final List<Image> images = new ArrayList<>();
	private final List<FontBlob> fontBlobs = new ArrayList<>();
	private final List<Font> fonts = new ArrayList<>();
	private final List<FakeImage> fakeImages = new ArrayList<>();

	public int addImage(BufferedImage image, Vk2dImageCompression compression, boolean pixelated, boolean clamped) {
		if (compression == Vk2dImageCompression.BC4) {
			throw new IllegalArgumentException("You need to use addGreyscaleImage for BC4 compression");
		}

		images.add(new Image(null, image, compression, null, pixelated, clamped));
		return images.size() - 1;
	}

	public int addGreyscaleImage(
			BufferedImage image, Vk2dImageCompression compression,
			Vk2dGreyscaleChannel channel, boolean pixelated, boolean clamped
	) {
		if (compression != Vk2dImageCompression.NONE && compression != Vk2dImageCompression.BC4) {
			throw new IllegalArgumentException("Unexpected greyscale image compression: " + compression);
		}
		images.add(new Image(null, image, compression, channel, pixelated, clamped));
		return images.size() - 1;
	}

	public int addPreCompressedImage(
			byte[] imageData, int width, int height,
			Vk2dImageCompression compression, boolean pixelated, boolean clamped
	) {
		images.add(new Image(imageData, new BufferedImage(
				width, height, BufferedImage.TYPE_BYTE_GRAY
		), compression, null, pixelated, clamped));
		return images.size() - 1;
	}

	public int addFontBlob(InputStream ttfInput) {
		int result = fonts.size();
		byte[] ttfBytes;
		try {
			ttfBytes = ttfInput.readAllBytes();
			ttfInput.close();
		} catch (IOException io) {
			throw new RuntimeException(io);
		}

		fontBlobs.add(new FontBlob(ttfBytes));
		return result;
	}

	public int addFont(int fontBlobIndex, int faceIndex) {
		int result = fonts.size();
		FontBlob fontData = fontBlobs.get(fontBlobIndex);

		var font = new Font(fontData, faceIndex);
		fonts.add(font);
		fontData.fonts.add(font);
		return result;
	}

	public void addFallbackAtlas(int fontIndex, int bitsPerPixel, float heightA, float maxRelativeDistance) {
		addAtlas(
				fontIndex, bitsPerPixel, heightA, maxRelativeDistance,
				0f, Float.MAX_VALUE,
				0f, Float.MAX_VALUE, null
		);
	}

	public void addAtlas(
			int fontIndex, int bitsPerPixel, float atlasHeightA, float maxRelativeDistance,
			float minRenderHeightA, float maxRenderHeightA,
			float minRelativeStrokeWidth, float maxRelativeStrokeWidth, String supportedCharacters
	) {
		var font = fonts.get(fontIndex);
		Set<Integer> supportedGlyphs = null;
		if (supportedCharacters != null) {
			supportedGlyphs = new HashSet<>();

			var data = font.fontData;
			if (data.hbBuffer == 0) {
				data.ttfBuffer = memCalloc(data.ttfBytes.length).put(0, data.ttfBytes);
				data.hbBlob = assertHbSuccess(hb_blob_create(
						data.ttfBuffer, HB_MEMORY_MODE_WRITABLE, 0L, null
				), "blob_create");
				data.hbBuffer = assertHbSuccess(hb_buffer_create(), "buffer_create");
			}

			if (font.hbFace == 0L) {
				font.hbFace = assertHbSuccess(hb_face_create(
						data.hbBlob, font.faceIndex
				), "face_create");
				font.hbFont = assertHbSuccess(hb_font_create(font.hbFace), "font_create");
			}

			hb_buffer_clear_contents(data.hbBuffer);

			try (var stack = MemoryStack.stackPush()) {
				var textBytes = stack.UTF8(supportedCharacters, false);
				hb_buffer_add_utf8(data.hbBuffer, textBytes, 0, textBytes.capacity());
			}

			hb_buffer_guess_segment_properties(data.hbBuffer);
			hb_shape(font.hbFont, data.hbBuffer, null);

			var glyphInfos = assertHbSuccess(
					hb_buffer_get_glyph_infos(data.hbBuffer),
					"buffer_get_glyph_infos"
			);
			for (var glyphInfo : glyphInfos) supportedGlyphs.add(glyphInfo.codepoint());
		}

		font.atlases.add(new SdfAtlas(
				bitsPerPixel, atlasHeightA, 1f / (atlasHeightA * maxRelativeDistance),
				minRenderHeightA, maxRenderHeightA,
				minRelativeStrokeWidth, maxRelativeStrokeWidth, supportedGlyphs
		));
	}

	public int addFakeImage(BufferedImage image, Vk2dFakeImageCompression compression) {
		ByteBuffer pixelBuffer = ByteBuffer.allocate(4 * image.getWidth() * image.getHeight());
		ImageCoding.encodeBufferedImage(pixelBuffer, image);
		pixelBuffer.flip();

		ByteBuffer data;
		if (compression == Vk2dFakeImageCompression.KIM1) {
			Kim1Compressor compressor = new Kim1Compressor(
					pixelBuffer, image.getWidth(), image.getHeight(), 4
			);
			data = BufferUtils.createByteBuffer(4 * compressor.intSize);
			compressor.compress(data);
		} else if (compression == Vk2dFakeImageCompression.KIM3) {
			Kim3Compressor compressor = new Kim3Compressor(pixelBuffer, image.getWidth(), image.getHeight());
			data = BufferUtils.createByteBuffer(4 * compressor.intSize);
			compressor.compress(data);
		} else throw new UnsupportedOperationException("TODO");

		data.flip();
		int[] intData =  new int[data.limit() / 4];
		for (int index = 0; index < intData.length; index++) {
			intData[index] = data.getInt();
		}
		fakeImages.add(new FakeImage(image.getWidth(), image.getHeight(), intData));
		return fakeImages.size() - 1;
	}

	public int addFakeImage(int width, int height, int[] imageData) {
		fakeImages.add(new FakeImage(width, height, Objects.requireNonNull(imageData)));
		return fakeImages.size() - 1;
	}

	private String computeImageHash(BufferedImage image) {
		try {
			ByteArrayOutputStream inputBytes = new ByteArrayOutputStream(
					8 + 4 * image.getWidth() * image.getHeight()
			);
			DataOutputStream dataBytes = new DataOutputStream(inputBytes);
			dataBytes.writeInt(image.getWidth());
			dataBytes.writeInt(image.getHeight());
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) dataBytes.writeInt(image.getRGB(x, y));
			}
			dataBytes.flush();
			dataBytes.close();

			MessageDigest computeHash = MessageDigest.getInstance("SHA-256");
			computeHash.update(inputBytes.toByteArray());
			byte[] byteHash = computeHash.digest();

			return HexFormat.of().formatHex(byteHash);
		} catch (Exception failed) {
			throw new RuntimeException(failed);
		}
	}

	private void loadBcImagesFromCache(File cacheDirectory) {
		if (cacheDirectory == null) return;
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		for (Image entry : images) {
			if (entry.compression != Vk2dImageCompression.BC7 || entry.data != null) continue;

			threadPool.submit(() -> {
				File expectedFile = new File(cacheDirectory + "/" + computeImageHash(entry.image) + ".bc7");
				if (expectedFile.exists()) {
					try {
						entry.data = Files.readAllBytes(expectedFile.toPath());
						entry.image = null;
					} catch (IOException failed) {
						throw new RuntimeException(failed);
					}
				}
			});
		}
		threadPool.close();
	}

	private void compressBc1AndBc4Images() {
		boolean hasBc1 = false;
		boolean hasBc4 = false;
		for (Image image : images) {
			if (image.compression == Vk2dImageCompression.BC1) hasBc1 = true;
			if (image.compression == Vk2dImageCompression.BC4) hasBc4 = true;
		}
		if (!hasBc1 && !hasBc4) return;

		BoilerInstance boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "Vk2dBc1/4Writer", 1
		).validation().forbidValidationErrors().doNotUseVma().defaultTimeout(100_000_000_000L).build();

		MemoryCombiner combiner = new MemoryCombiner(boiler, "Bc1/4CompressionMemory");
		Bc1Compressor compressor1 = hasBc1 ? new Bc1Compressor(boiler, combiner, combiner) : null;
		Bc4Compressor compressor4 = hasBc4 ? new Bc4Compressor(boiler) : null;

		int maxDestinationImagePixels = 0;
		List<MappedVkbBuffer> sourceBuffers = new ArrayList<>();
		List<MappedVkbBuffer> destinationBuffers = new ArrayList<>();
		long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		for (var compression : new Vk2dImageCompression[] { Vk2dImageCompression.BC1, Vk2dImageCompression.BC4 }) {
			for (Image entry : images) {
				if (entry.compression != compression || entry.data != null) continue;

				long paddedWidth = nextMultipleOf(entry.image.getWidth(), 4);
				long paddedHeight = nextMultipleOf(entry.image.getHeight(), 4);
				long sourceSize = paddedWidth * paddedHeight;
				if (entry.compression == Vk2dImageCompression.BC1) sourceSize *= 4;

				sourceBuffers.add(combiner.addMappedBuffer(sourceSize, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT));
				destinationBuffers.add(combiner.addMappedBuffer(
						paddedWidth * paddedHeight / 2L, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
				));
				maxDestinationImagePixels = toIntExact(max(
						maxDestinationImagePixels, paddedWidth * paddedHeight
				));
			}
		}

		MemoryBlock memory = combiner.build(false);

		Bc1Worker worker1 = hasBc1 ? new Bc1Worker(compressor1, maxDestinationImagePixels, combiner) : null;

		DescriptorCombiner descriptors = new DescriptorCombiner(boiler);
		long[] descriptorSets1 = compressor1 != null ?
				descriptors.addMultiple(compressor1.descriptorSetLayout, destinationBuffers.size()) : null;
		long[] descriptorSets4 = compressor4 != null ?
				descriptors.addMultiple(compressor4.descriptorSetLayout, destinationBuffers.size()) : null;
		long descriptorPool = descriptors.build("Vk2dBc1/4DescriptorPool");

		SingleTimeCommands.submit(boiler, "Vk2dBc1/4Compression", recorder -> {
			if (compressor1 != null) compressor1.performStagingTransfer(recorder);

			int imageIndex = 0;
			if (worker1 != null) worker1.bindPipeline(recorder);
			for (Image entry : images) {
				if (entry.compression == Vk2dImageCompression.BC1) {
					MappedVkbBuffer source = sourceBuffers.get(imageIndex);
					ByteBuffer sourceBytes = source.byteBuffer();
					int paddedWidth = nextMultipleOf(entry.image.getWidth(), 4);
					int paddedHeight = nextMultipleOf(entry.image.getHeight(), 4);
					for (int y = 0; y < paddedHeight; y++) {
						for (int x = 0; x < paddedWidth; x++) {
							int sourceX = Math.min(x, entry.image.getWidth() - 1);
							int sourceY = Math.min(y, entry.image.getHeight() - 1);
							Color color = new Color(entry.image.getRGB(sourceX, sourceY), true);

							sourceBytes.put((byte) color.getRed());
							sourceBytes.put((byte) color.getGreen());
							sourceBytes.put((byte) color.getBlue());
							sourceBytes.put((byte) color.getAlpha());
						}
					}

					MappedVkbBuffer destination = destinationBuffers.get(imageIndex);
					assert worker1 != null && descriptorSets1 != null;
					worker1.compress(
							recorder, descriptorSets1[imageIndex], source,
							destination, paddedWidth, paddedHeight
					);
					imageIndex += 1;
				}
			}

			if (compressor4 != null) compressor4.bindPipeline(recorder);
			for (Image entry : images) {
				if (entry.compression == Vk2dImageCompression.BC4) {
					MappedVkbBuffer source = sourceBuffers.get(imageIndex);
					ByteBuffer sourceBytes = source.byteBuffer();
					int paddedWidth = nextMultipleOf(entry.image.getWidth(), 4);
					int paddedHeight = nextMultipleOf(entry.image.getHeight(), 4);
					for (int y = 0; y < paddedHeight; y++) {
						for (int x = 0; x < paddedWidth; x++) {
							int sourceX = Math.min(x, entry.image.getWidth() - 1);
							int sourceY = Math.min(y, entry.image.getHeight() - 1);
							Color color = new Color(entry.image.getRGB(sourceX, sourceY), true);

							byte greyscale = switch (entry.channel) {
								case Vk2dGreyscaleChannel.RGB -> (byte) ((color.getRed() + color.getGreen() + color.getBlue()) / 3);
								case Vk2dGreyscaleChannel.ALPHA -> (byte) color.getAlpha();
								case Vk2dGreyscaleChannel.RED -> (byte) color.getRed();
							};
							sourceBytes.put(greyscale);
						}
					}

					MappedVkbBuffer destination = destinationBuffers.get(imageIndex);
					assert compressor4 != null && descriptorSets4 != null;
					compressor4.compress(
							recorder, descriptorSets4[imageIndex], source,
							destination, paddedWidth, paddedHeight, false
					);
					imageIndex += 1;
				}
			}
		}).destroy();

		int bcIndex = 0;
		for (Image entry : images) {
			if (entry.compression != Vk2dImageCompression.BC1 && entry.compression != Vk2dImageCompression.BC4) {
				continue;
			}

			MappedVkbBuffer destination = destinationBuffers.get(bcIndex);
			entry.data = new byte[Math.toIntExact(destination.size)];
			destination.byteBuffer().get(entry.data);
			bcIndex += 1;
		}
		if (compressor1 != null) compressor1.destroy();
		if (compressor4 != null) compressor4.destroy();
		memory.destroy(boiler);
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null);
	}

	private void compressBc7Images() {
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		for (Image entry : images) {
			if (entry.compression != Vk2dImageCompression.BC7) continue;

			if (entry.data == null) {
				threadPool.submit(() -> entry.data = Bc7Compressor.compressBc7(entry.image));
			}
		}
		threadPool.close();

		for (Image entry : images) {
			if (entry.compression == Vk2dImageCompression.BC7 && entry.data == null) {
				throw new RuntimeException("BC7 compression apparently failed");
			}
		}
	}

	private void saveBcImagesToCache(File cacheDirectory) {
		if (cacheDirectory == null) return;
		for (Image entry : images) {
			if (entry.compression != Vk2dImageCompression.BC7 || entry.image == null) continue;

			File cached = new File(cacheDirectory + "/" + computeImageHash(entry.image) + ".bc7");
			if (!cached.exists()) {
				if (!cacheDirectory.isDirectory()) {
					if (!cacheDirectory.mkdirs() && !cacheDirectory.isDirectory()) {
						throw new RuntimeException("Failed to create " + cacheDirectory);
					}
				}

				try {
					Files.write(cached.toPath(), entry.data);
				} catch (IOException failed) {
					throw new RuntimeException(failed);
				}
			}
		}
	}

	public void write(OutputStream rawOutput, File cacheDirectory) throws IOException {
		var uncompressedOutput = new ByteArrayOutputStream();
		DataOutputStream output = new DataOutputStream(uncompressedOutput);
		output.writeInt(images.size());
		for (Image entry : images) {
			output.writeInt(entry.image.getWidth());
			output.writeInt(entry.image.getHeight());

			if (entry.compression == Vk2dImageCompression.NONE && entry.channel != null) {
				output.writeByte((byte) -1);
			} else {
				output.writeByte(entry.compression.ordinal());
			}
			output.writeByte(entry.pixelated ? 1 : 0);
			output.writeByte(entry.clamped ? 1 : 0);
		}

		output.writeInt(fakeImages.size());
		for (FakeImage image : fakeImages) {
			output.writeInt(image.data.length);
			output.writeInt(image.width);
			output.writeInt(image.height);
		}

		output.writeInt(fontBlobs.size());
		for (var data : fontBlobs) {
			output.writeInt(data.ttfBytes.length);
			output.write(data.ttfBytes);
			output.writeInt(data.fonts.size());
			for (var font : data.fonts) {
				output.writeInt(font.faceIndex);
				output.writeInt(font.atlases.size());
				for (var atlas : font.atlases) {
					if (atlas.supportedGlyphs != null) {
						output.writeInt(atlas.supportedGlyphs.size());
						for (int glyph : atlas.supportedGlyphs) {
							output.writeInt(glyph);
						}
					} else output.writeInt(-1);

					output.write(atlas.bitsPerPixel);
					output.writeFloat(atlas.heightA);
					output.writeFloat(atlas.distanceScale);
					output.writeFloat(atlas.minHeightA);
					output.writeFloat(atlas.maxHeightA);
					output.writeFloat(atlas.minRelativeStrokeWidth);
					output.writeFloat(atlas.maxRelativeStrokeWidth);
				}

				if (font.hbFont != 0L) {
					hb_font_destroy(font.hbFont);
					hb_face_destroy(font.hbFace);
				}
			}

			if (data.ttfBuffer != null) {
				memFree(data.ttfBuffer);
				data.ttfBuffer = null;
				hb_buffer_destroy(data.hbBuffer);
				hb_blob_destroy(data.hbBlob);
			}
		}

		loadBcImagesFromCache(cacheDirectory);
		compressBc1AndBc4Images();
		compressBc7Images();
		saveBcImagesToCache(cacheDirectory);

		for (Image entry : images) {
			switch (entry.compression) {
				case Vk2dImageCompression.NONE:
					writeUncompressedImage(output, entry.image, entry.channel);
					break;
				case Vk2dImageCompression.BC1:
				case Vk2dImageCompression.BC4:
				case Vk2dImageCompression.BC7:
					output.write(entry.data);
					break;
				default:
					throw new UnsupportedOperationException("Unexpected compression " + entry.compression);
			}
		}

		for (FakeImage image : fakeImages) {
			for (int value : image.data) output.writeInt(value);
		}

		output.flush();

		var uncompressedByteArray = uncompressedOutput.toByteArray();
		var uncompressedByteBuffer = memCalloc(uncompressedByteArray.length);
		uncompressedByteBuffer.put(0, uncompressedByteArray);

		var compressedByteBuffer = memCalloc(Math.toIntExact(ZSTD_compressBound(uncompressedByteArray.length)));
		long startCompression = System.nanoTime();
		// TODO CHAP3 Use a higher compression level
		int compressedSize = Math.toIntExact(ZSTD_compress(compressedByteBuffer, uncompressedByteBuffer, 7));
		System.out.println("compression took " + (System.nanoTime() - startCompression) / 1000_000L + " ms");
		memFree(uncompressedByteBuffer);
		var compressedByteArray = new byte[compressedSize];
		compressedByteBuffer.get(compressedByteArray);
		memFree(compressedByteBuffer);
		rawOutput.write(compressedByteArray);
		rawOutput.flush();
	}

	public Vk2dResourceBundle directlyCreateBundle(Vk2dInstance instance, File cacheDirectory) {
		try {
			var output = new ByteArrayOutputStream();
			write(output, cacheDirectory);
			var input = new ByteArrayInputStream(output.toByteArray());
			return Vk2dResourceLoader.loadSimple(instance, input);
		} catch (IOException io) {
			throw new RuntimeException(io);
		}
	}

	private void writeUncompressedImage(
			DataOutputStream output, BufferedImage image, Vk2dGreyscaleChannel channel
	) throws IOException {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Color color = new Color(image.getRGB(x, y), true);
				if (channel == null) {
					output.writeByte(color.getRed());
					output.writeByte(color.getGreen());
					output.writeByte(color.getBlue());
					output.writeByte(color.getAlpha());
				} else {
					byte greyscale = switch (channel) {
						case Vk2dGreyscaleChannel.RGB -> (byte) ((color.getRed() + color.getGreen() + color.getBlue()) / 3);
						case Vk2dGreyscaleChannel.ALPHA -> (byte) color.getAlpha();
						case Vk2dGreyscaleChannel.RED -> (byte) color.getRed();
					};
					output.writeByte(greyscale);
				}
			}
		}
	}

	private static class Image {

		byte[] data;
		BufferedImage image;
		final Vk2dImageCompression compression;
		final Vk2dGreyscaleChannel channel;
		final boolean pixelated;
		final boolean clamped;

		Image(
				byte[] data, BufferedImage image, Vk2dImageCompression compression,
				Vk2dGreyscaleChannel channel, boolean pixelated, boolean clamped
		) {
			this.data = data;
			this.image = image;
			this.compression = compression;
			this.channel = channel;
			this.pixelated = pixelated;
			this.clamped = clamped;
		}
	}

	private record FakeImage(int width, int height, int[] data) {}

	private class FontBlob {

		final byte[] ttfBytes;
		final List<Font> fonts = new ArrayList<>();

		ByteBuffer ttfBuffer;
		long hbBlob, hbBuffer;

		FontBlob(byte[] ttfBytes) {
			this.ttfBytes = ttfBytes;
		}
	}

	private class Font {

		final FontBlob fontData;
		final int faceIndex;
		final List<SdfAtlas> atlases = new ArrayList<>();

		long hbFace, hbFont;

		Font(FontBlob fontData, int faceIndex) {
			this.fontData = fontData;
			this.faceIndex = faceIndex;
		}
	}

	private record SdfAtlas(
			int bitsPerPixel, float heightA, float distanceScale,
			float minHeightA, float maxHeightA,
			float minRelativeStrokeWidth, float maxRelativeStrokeWidth,
			Set<Integer> supportedGlyphs
	) {}
}
