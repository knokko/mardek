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
import com.github.knokko.vk2d.Kim3Compressor;
import org.lwjgl.BufferUtils;
import org.lwjgl.CLongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Outline_Funcs;
import org.lwjgl.util.freetype.FT_Vector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DeflaterOutputStream;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static com.github.knokko.vk2d.text.FontHelper.assertFtSuccess;
import static java.lang.Math.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dResourceWriter {

	private long ftLibrary;

	private final List<Image> images = new ArrayList<>();
	private final List<Font> fonts = new ArrayList<>();
	private final List<FakeImage> fakeImages = new ArrayList<>();

	public int addImage(BufferedImage image, Vk2dImageCompression compression, boolean pixelated) {
		if (compression == Vk2dImageCompression.BC4) {
			throw new IllegalArgumentException("You need to use addGreyscaleImage for BC4 compression");
		}
		if (compression == Vk2dImageCompression.BC1 && (image.getWidth() % 4 != 0 || image.getHeight() % 4 != 0)) {
			throw new UnsupportedOperationException(
					"We only support BC1 compression for images whose width and height are multiples of 4"
			);
		}
		images.add(new Image(null, image, compression, null, pixelated));
		return images.size() - 1;
	}

	public int addGreyscaleImage(
			BufferedImage image, Vk2dImageCompression compression,
			Vk2dGreyscaleChannel channel, boolean pixelated
	) {
		if (compression != Vk2dImageCompression.NONE && compression != Vk2dImageCompression.BC4) {
			throw new IllegalArgumentException("Unexpected greyscale image compression: " + compression);
		}
		images.add(new Image(null, image, compression, channel, pixelated));
		return images.size() - 1;
	}

	public int addPreCompressedImage(
			byte[] imageData, int width, int height,
			Vk2dImageCompression compression, boolean pixelated
	) {
		images.add(new Image(imageData, new BufferedImage(
				width, height, BufferedImage.TYPE_BYTE_GRAY
		), compression, null, pixelated));
		return images.size() - 1;
	}

	public int addFont(InputStream ttfInput) {
		byte[] fontBytes;
		try {
			fontBytes = ttfInput.readAllBytes();
			ttfInput.close();
		} catch (IOException io) {
			throw new RuntimeException(io);
		}

		ByteBuffer ttfBuffer = memAlloc(fontBytes.length);
		ttfBuffer.put(0, fontBytes);

		FT_Face font;
		try (MemoryStack stack = stackPush()) {
			if (ftLibrary == 0L) {
				PointerBuffer pLibrary = stack.callocPointer(1);
				assertFtSuccess(FT_Init_FreeType(pLibrary), "Init_FreeType");
				ftLibrary = pLibrary.get(0);
			}

			PointerBuffer pFace = stack.callocPointer(1);
			assertFtSuccess(FT_New_Memory_Face(
					ftLibrary, ttfBuffer, 0, pFace
			), "New_Memory_Face");
			font = FT_Face.create(pFace.get(0));
		}

		int result = addFont(font);
		assertFtSuccess(FT_Done_Face(font), "Done_Face");
		memFree(ttfBuffer);
		return result;
	}

	private static int transformFont(long value, int minValue, int maxValue) {
		int worldSize = maxValue - minValue;
		int relativeValue = Math.toIntExact(value) - minValue;
		if (relativeValue < 0) {
			System.out.println("Too low");
			return 0;
		}
		if (relativeValue > worldSize) {
			System.out.println("Too high");
			return 1023;
		}
		// TODO DL Tune this again
		return Math.toIntExact(relativeValue * 1023L / worldSize);
	}

	public int addFont(FT_Face font) {
		record RawCurve(long startX, long startY, long controlX, long controlY, long endX, long endY) {}

		record RawGlyph(int firstCurve, int numCurves, long minX, long minY, long maxX, long maxY, long advance) {}

		RawGlyph[] rawGlyphs = new RawGlyph[Math.toIntExact(font.num_glyphs())];
		List<RawCurve> rawCurves = new ArrayList<>();
		int glyphA = FT_Get_Char_Index(font, 'A');

		int maxCurves = 0;
		try (MemoryStack stack = stackPush()) {
			var position = FT_Vector.calloc(stack);
			var outlineFunctions = FT_Outline_Funcs.calloc(stack);

			outlineFunctions.move_to((long raw, long userData) -> {
				@SuppressWarnings("resource") var to = FT_Vector.create(raw);
				position.x(to.x());
				position.y(to.y());
				return 0;
			});
			outlineFunctions.line_to((long raw, long userData) -> {
				@SuppressWarnings("resource") var to = FT_Vector.create(raw);
				rawCurves.add(new RawCurve(
						position.x(), position.y(),
						(position.x() + to.x()) / 2L,
						(position.y() + to.y()) / 2L,
						to.x(), to.y()
				));
				position.x(to.x());
				position.y(to.y());
				return 0;
			});
			outlineFunctions.conic_to((long rawControl, long rawTo, long userData) -> {
				@SuppressWarnings("resource") var control = FT_Vector.create(rawControl);
				@SuppressWarnings("resource") var to = FT_Vector.create(rawTo);
				rawCurves.add(new RawCurve(
						position.x(), position.y(),
						control.x(), control.y(),
						to.x(), to.y()
				));
				position.x(to.x());
				position.y(to.y());
				return 0;
			});
			outlineFunctions.cubic_to((long control1, long control2, long to, long userData) -> 1);
			outlineFunctions.delta(0);
			outlineFunctions.shift(0);

			CLongBuffer pAdvance = stack.callocCLong(1);
			for (int glyph = 0; glyph < font.num_glyphs(); glyph++) {
				int curveIndex = rawCurves.size();
				assertFtSuccess(FT_Load_Glyph(font, glyph, FT_LOAD_NO_SCALE), "Load_Glyph");
				var outline = Objects.requireNonNull(font.glyph()).outline();
				assertFtSuccess(FT_Outline_Decompose(outline, outlineFunctions, 0L), "Outline_Decompose");
				int numCurves = rawCurves.size() - curveIndex;
				maxCurves = max(maxCurves, numCurves);

				long minX = Long.MAX_VALUE;
				long minY = Long.MAX_VALUE;
				long maxX = Long.MIN_VALUE;
				long maxY = Long.MIN_VALUE;
				for (int index = curveIndex; index < curveIndex + numCurves; index++) {
					RawCurve curve = rawCurves.get(index);
					minX = min(minX, curve.startX);
					maxX = max(maxX, curve.startX);
					minY = min(minY, curve.startY);
					maxY = max(maxY, curve.startY);
					minX = min(minX, curve.controlX);
					maxX = max(maxX, curve.controlX);
					minY = min(minY, curve.controlY);
					maxY = max(maxY, curve.controlY);
					minX = min(minX, curve.endX);
					maxX = max(maxX, curve.endX);
					minY = min(minY, curve.endY);
					maxY = max(maxY, curve.endY);
				}

				assertFtSuccess(FT_Get_Advance(
						font, glyph, FT_LOAD_NO_SCALE, pAdvance
				), "Get_Advance");
				rawGlyphs[glyph] = new RawGlyph(curveIndex, numCurves, minX, minY, maxX, maxY, pAdvance.get(0));
			}
		}

		int heightA = Math.toIntExact(rawGlyphs[glyphA].maxY);
		int minY = -heightA / 2;
		int maxY = 2 * heightA;
		FontCurve[] curves = new FontCurve[rawCurves.size()];
		for (int index = 0; index < curves.length; index++) {
			RawCurve raw = rawCurves.get(index);
			int startX = transformFont(raw.startX, minY, maxY);
			int controlX = transformFont(raw.controlX, minY, maxY);
			int endX = transformFont(raw.endX, minY, maxY);
			int packedX = startX | (controlX << 10) | (endX << 20);
			int startY = transformFont(raw.startY, minY, maxY);
			int controlY = transformFont(raw.controlY, minY, maxY);
			int endY = transformFont(raw.endY, minY, maxY);
			int packedY = startY | (controlY << 10) | (endY << 20);
			curves[index] = new FontCurve(packedX, packedY);
		}

		FontGlyph[] glyphs = new FontGlyph[rawGlyphs.length];
		for (int index = 0; index < glyphs.length; index++) {
			RawGlyph raw = rawGlyphs[index];
			glyphs[index] = new FontGlyph(
					raw.firstCurve, raw.numCurves,
					(float) raw.minX / heightA, (float) raw.minY / heightA,
					(float) raw.maxX / heightA, (float) raw.maxY / heightA,
					(float) raw.advance / heightA
			);
		}
		System.out.println("max curves is " + maxCurves + " and heightA is " + heightA);

		Map<Integer, Integer> charToGlyphMap = new HashMap<>();
		try (MemoryStack stack = stackPush()) {
			IntBuffer pGlyph = stack.callocInt(1);
			long charCode = FT_Get_First_Char(font, pGlyph);
			while (charCode > 0L) {
				charToGlyphMap.put(Math.toIntExact(charCode), pGlyph.get(0));
				charCode = FT_Get_Next_Char(font, charCode, pGlyph);
			}
		}

		fonts.add(new Font(font, curves, glyphs, charToGlyphMap));
		return fonts.size() - 1;
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
		).validation().forbidValidationErrors().doNotUseVma().build();

		MemoryCombiner combiner = new MemoryCombiner(boiler, "Bc1/4CompressionMemory");
		Bc1Compressor compressor1 = hasBc1 ? new Bc1Compressor(boiler, combiner, combiner) : null;
		// TODO DL Fix validation warning "vkCmdBindPipeline(): [AMD] [NVIDIA] Pipeline VkPipeline 0x50000000005[Bc4Compressor] was bound twice in the frame."
		Bc4Compressor compressor4 = hasBc4 ? new Bc4Compressor(boiler) : null;

		int maxDestinationImagePixels = 0;
		List<MappedVkbBuffer> sourceBuffers = new ArrayList<>();
		List<MappedVkbBuffer> destinationBuffers = new ArrayList<>();
		long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		for (Image entry : images) {
			if (entry.data != null) continue;
			if (entry.compression != Vk2dImageCompression.BC1 && entry.compression != Vk2dImageCompression.BC4) {
				continue;
			}

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
		MemoryBlock memory = combiner.build(false);

		Bc1Worker worker1 = hasBc1 ? new Bc1Worker(compressor1, maxDestinationImagePixels, combiner) : null;
		Bc4Worker worker4 = hasBc4 ? new Bc4Worker(compressor4, maxDestinationImagePixels, combiner) : null;

		DescriptorCombiner descriptors = new DescriptorCombiner(boiler);
		long[] descriptorSets1 = compressor1 != null ?
				descriptors.addMultiple(compressor1.descriptorSetLayout, destinationBuffers.size()) : null;
		long[] descriptorSets4 = compressor4 != null ?
				descriptors.addMultiple(compressor4.descriptorSetLayout, destinationBuffers.size()) : null;
		long descriptorPool = descriptors.build("Vk2dBc1/4DescriptorPool");

		SingleTimeCommands.submit(boiler, "Vk2dBc1/4Compression", recorder -> {
			if (compressor1 != null) compressor1.performStagingTransfer(recorder);

			int imageIndex = 0;
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
					assert worker4 != null && descriptorSets4 != null;
					worker4.compress(
							recorder, descriptorSets4[imageIndex], source,
							destination, paddedWidth, paddedHeight
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
		DeflaterOutputStream deflate = new DeflaterOutputStream(rawOutput);
		DataOutputStream output = new DataOutputStream(deflate);
		output.writeInt(images.size());
		for (Image entry : images) {
			output.writeInt(entry.image.getWidth());
			output.writeInt(entry.image.getHeight());
			output.writeByte(entry.compression.ordinal());
			output.writeByte(entry.pixelated ? 1 : 0);
		}

		output.writeInt(fakeImages.size());
		for (FakeImage image : fakeImages) {
			output.writeInt(image.data.length);
			output.writeInt(image.width);
			output.writeInt(image.height);
		}

		output.writeInt(fonts.size());
		for (Font font : fonts) {
			output.writeInt(font.curves.length);
			output.writeInt(font.glyphs.length);
			for (FontGlyph glyph : font.glyphs) {
				output.writeInt(glyph.curveIndex);
				output.writeInt(glyph.numCurves);
				output.writeFloat(glyph.minX);
				output.writeFloat(glyph.minY);
				output.writeFloat(glyph.maxX);
				output.writeFloat(glyph.maxY);
				output.writeFloat(glyph.advance);
			}
			output.writeInt(font.charToGlyphMap.size());
			for (var entry : font.charToGlyphMap.entrySet()) {
				output.writeInt(entry.getKey());
				output.writeInt(entry.getValue());
			}
		}

		loadBcImagesFromCache(cacheDirectory);
		compressBc1AndBc4Images();
		compressBc7Images();
		saveBcImagesToCache(cacheDirectory);

		for (Image entry : images) {
			switch (entry.compression) {
				case Vk2dImageCompression.NONE:
					writeUncompressedImage(output, entry.image);
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

		for (Font font : fonts) {
			for (FontCurve curve : font.curves) {
				output.writeInt(curve.packedX);
				output.writeInt(curve.packedY);
			}
		}

		deflate.finish();
		output.flush();
		if (ftLibrary != 0L) {
			assertFtSuccess(FT_Done_FreeType(ftLibrary), "Done_FreeType");
		}
	}

	private void writeUncompressedImage(DataOutputStream output, BufferedImage image) throws IOException {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Color color = new Color(image.getRGB(x, y), true);
				output.writeByte(color.getRed());
				output.writeByte(color.getGreen());
				output.writeByte(color.getBlue());
				output.writeByte(color.getAlpha());
			}
		}
	}

	private static class Image {

		byte[] data;
		BufferedImage image;
		final Vk2dImageCompression compression;
		final Vk2dGreyscaleChannel channel;
		final boolean pixelated;

		Image(
				byte[] data, BufferedImage image, Vk2dImageCompression compression,
				Vk2dGreyscaleChannel channel, boolean pixelated
		) {
			this.data = data;
			this.image = image;
			this.compression = compression;
			this.channel = channel;
			this.pixelated = pixelated;
		}
	}

	private record FakeImage(int width, int height, int[] data) {}

	private record Font(FT_Face ftFace, FontCurve[] curves, FontGlyph[] glyphs, Map<Integer, Integer> charToGlyphMap) {}

	private record FontCurve(int packedX, int packedY) {}

	private record FontGlyph(int curveIndex, int numCurves, float minX, float minY, float maxX, float maxY, float advance) {}
}
