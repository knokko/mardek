package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.utilities.ImageCoding;
import com.github.knokko.compressor.Bc1Compressor;
import com.github.knokko.compressor.Bc1Worker;
import com.github.knokko.compressor.Bc7Compressor;
import com.github.knokko.compressor.Kim1Compressor;
import com.github.knokko.vk2d.Kim3Compressor;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Outline_Funcs;
import org.lwjgl.util.freetype.FT_Vector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
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
	private byte[][] bcImageData;

	public int addImage(BufferedImage image, Vk2dImageCompression compression, boolean pixelated) {
		if (compression == Vk2dImageCompression.BC1 && (image.getWidth() % 4 != 0 || image.getHeight() % 4 != 0)) {
			throw new UnsupportedOperationException(
					"We only support BC1 compression for images whose width and height are multiples of 4"
			);
		}
		images.add(new Image(image, compression, pixelated));
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
				assertFtSuccess(FT_Init_FreeType(pLibrary), "Init_FreeType", "Vk2dResourceWriter");
				ftLibrary = pLibrary.get(0);
			}

			PointerBuffer pFace = stack.callocPointer(1);
			assertFtSuccess(FT_New_Memory_Face(
					ftLibrary, ttfBuffer, 0, pFace
			), "New_Memory_Face", "Vk2dResourceWriter");
			font = FT_Face.create(pFace.get(0));
		}

		int result = addFont(font);
		assertFtSuccess(FT_Done_Face(font), "Done_Face", "Vk2dResourceWriter");
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
		return Math.toIntExact(relativeValue * 1023L / worldSize);
	}

	private void playground(FontCurve[] packedCurves, FontGlyph glyph) throws IOException {
		DataOutputStream intersectionOutput = new DataOutputStream(Files.newOutputStream(new File("intersections.bin").toPath()));
		DataOutputStream infoOutput = new DataOutputStream(Files.newOutputStream(new File("info.bin").toPath()));
		int outerIntersectionIndex = 0;

		record Curve(float startX, float startY, float controlX, float controlY, float endX, float endY) { }

		Curve[] curves = new Curve[packedCurves.length];
		for (int index = 0; index < curves.length; index++) {
			FontCurve raw = packedCurves[index];
			curves[index] = new Curve(
					(raw.packedX & 1023) * 2.5f / 1023f - 0.5f,
					(raw.packedY & 1023) * 2.5f / 1023f - 0.5f,
					((raw.packedX >> 10) & 1023) * 2.5f / 1023f - 0.5f,
					((raw.packedY >> 10) & 1023) * 2.5f / 1023f - 0.5f,
					((raw.packedX >> 20) & 1023) * 2.5f / 1023f - 0.5f,
					((raw.packedY >> 20) & 1023) * 2.5f / 1023f - 0.5f
			);
		}

		int pixelHeight = 100;
		float glyphHeight = glyph.maxY - glyph.minY;
		for (int pixelY = 0; pixelY < pixelHeight; pixelY++) {
			float glyphY = glyph.minY + (pixelY + 0.5f) * glyphHeight / pixelHeight;
			System.out.print("glyphY is " + glyphY + " with intersections ");

			int intersectionIndex = 0;
			float[] intersections = new float[2 * curves.length];
			for (Curve curve : curves) {
				float startY = curve.startY - glyphY;
				float controlY = curve.controlY - glyphY;
				float endY = curve.endY - glyphY;

				float cutoff = 0.5f * glyphHeight / pixelHeight;
				if (startY < -cutoff && controlY < -cutoff && endY < -cutoff) continue;
				if (startY > cutoff && controlY > cutoff && endY > cutoff) continue;

				float a = startY - 2f * controlY + endY;
				float b = startY - controlY;
				float t0, t1;
				if (abs(a) > 1e-5f) {
					// Quadratic segment, solve abc formula to find roots.
					float radicand = b * b - a * startY;
					if (radicand <= 0) continue;

					float s = (float) sqrt(radicand);
					t0 = (b - s) / a;
					t1 = (b + s) / a;
				} else {
					// Linear segment, avoid division by a.y, which is near zero.
					float t = startY / (startY - endY);
					t0 = t;
					t1 = t;
				}

				float ax = curve.startX - 2f * curve.controlX + curve.endX;
				float bx = curve.startX - curve.controlX;
				float cx = curve.startX;
				float x = (ax * t0 - 2f * bx) * t0 + cx;
				if (!((x < curve.startX && x < curve.controlX && x < curve.endX) || (x > curve.startX && x > curve.controlX && x > curve.endX))) {
					intersections[intersectionIndex++] = x;
					System.out.print(x + " ");
				}

				if (t0 != t1) {
					x = (ax * t1 - 2f * bx) * t1 + cx;
					if (!((x < curve.startX && x < curve.controlX && x < curve.endX) || (x > curve.startX && x > curve.controlX && x > curve.endX))) {
						System.out.print(x + " ");
						intersections[intersectionIndex++] = x;
					}
				}
			}
			System.out.println();
			for (int i = 1; i < intersectionIndex; i++) {
				float old = intersections[0 + i];
				int j;
				for (j = i; j > 0 && intersections[0 + j - 1] > old; j--) {
					intersections[0 + j] = intersections[0 + j - 1];
				}
				intersections[0 + j] = old;
			}
			//Arrays.sort(intersections);
			infoOutput.writeInt(outerIntersectionIndex);
			infoOutput.writeInt(intersectionIndex);
			outerIntersectionIndex += intersectionIndex;
			System.out.println("sorted is " + Arrays.toString(Arrays.copyOf(intersections, intersectionIndex)));
			for (float intersection : Arrays.copyOf(intersections, intersectionIndex)) intersectionOutput.writeFloat(intersection);
		}

		infoOutput.flush();
		intersectionOutput.flush();
		infoOutput.close();
		intersectionOutput.close();
	}

	public int addFont(FT_Face font) {
		record RawCurve(long startX, long startY, long controlX, long controlY, long endX, long endY) {}

		record RawGlyph(int firstCurve, int numCurves, long minX, long minY, long maxX, long maxY) {}

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

			for (int glyph = 0; glyph < font.num_glyphs(); glyph++) {
				int curveIndex = rawCurves.size();
				assertFtSuccess(FT_Load_Glyph(font, glyph, FT_LOAD_NO_SCALE), "Load_Glyph", "Vk2dResourceWriter");
				var outline = Objects.requireNonNull(font.glyph()).outline();
				assertFtSuccess(FT_Outline_Decompose(outline, outlineFunctions, 0L), "Outline_Decompose", "Vk2dResourceWriter");
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
				rawGlyphs[glyph] = new RawGlyph(curveIndex, numCurves, minX, minY, maxX, maxY);
			}
		}

		int heightA = Math.toIntExact(rawGlyphs[glyphA].maxY);
		int minY = -heightA / 2;
		int maxY = 2 * heightA;
		System.out.println(heightA);
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
					(float) raw.maxX / heightA, (float) raw.maxY / heightA
			);
			System.out.println("glyph " + index + " has " + raw.numCurves + " curves starting at " + raw.firstCurve);
		}

		FontGlyph testGlyph = glyphs[4];
		try {
			playground(Arrays.copyOfRange(curves, testGlyph.curveIndex, testGlyph.curveIndex + testGlyph.numCurves), testGlyph);
		} catch (IOException failed) {
			throw new RuntimeException(failed);
		}
		System.out.println("max curves is " + maxCurves + " and heightA is " + heightA);

		fonts.add(new Font(font, curves, glyphs));
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

	private void compressBc1Images() {
		if (images.stream().noneMatch(entry -> entry.compression == Vk2dImageCompression.BC1)) return;

		BoilerInstance boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "Vk2dBc1Writer", 1
		).validation().forbidValidationErrors().build();

		// TODO update vk-compressor docs (or replace entirely)
		MemoryCombiner combiner = new MemoryCombiner(boiler, "Bc1CompressionMemory");
		Bc1Compressor compressor = new Bc1Compressor(boiler, combiner, combiner);

		int maxDestinationImagePixels = 0;
		List<MappedVkbBuffer> sourceBuffers = new ArrayList<>();
		List<MappedVkbBuffer> destinationBuffers = new ArrayList<>();
		long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		for (Image entry : images) {
			if (entry.compression != Vk2dImageCompression.BC1) continue;

			sourceBuffers.add(combiner.addMappedBuffer(
					4L * entry.image.getWidth() * entry.image.getHeight(),
					alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
			));
			destinationBuffers.add(combiner.addMappedBuffer(
					(long) entry.image.getWidth() * entry.image.getHeight() / 2,
					alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
			));
			maxDestinationImagePixels = max(
					maxDestinationImagePixels, entry.image.getWidth() * entry.image.getHeight()
			);
		}
		MemoryBlock memory = combiner.build(false);

		Bc1Worker worker = new Bc1Worker(compressor, maxDestinationImagePixels, combiner);

		DescriptorCombiner descriptors = new DescriptorCombiner(boiler);
		long[] descriptorSets = descriptors.addMultiple(compressor.descriptorSetLayout, destinationBuffers.size());
		long descriptorPool = descriptors.build("Vk2dBc1DescriptorPool");

		SingleTimeCommands.submit(boiler, "Vk2dBc1Compression", recorder -> {
			compressor.performStagingTransfer(recorder);

			int imageIndex = 0;
			for (Image entry : images) {
				if (entry.compression != Vk2dImageCompression.BC1) continue;

				MappedVkbBuffer source = sourceBuffers.get(imageIndex);
				ImageCoding.encodeBufferedImage(source.byteBuffer(), entry.image);
				MappedVkbBuffer destination = destinationBuffers.get(imageIndex);
				worker.compress(
						recorder, descriptorSets[imageIndex], source,
						destination, entry.image.getWidth(), entry.image.getHeight()
				);
				imageIndex += 1;
			}
		}).destroy();

		int bcIndex = 0;
		int imageIndex = -1;
		for (Image entry : images) {
			imageIndex += 1;
			if (entry.compression != Vk2dImageCompression.BC1) continue;

			MappedVkbBuffer destination = destinationBuffers.get(bcIndex);
			this.bcImageData[imageIndex] = new byte[Math.toIntExact(destination.size)];
			destination.byteBuffer().get(this.bcImageData[imageIndex]);
			bcIndex += 1;
		}
		compressor.destroy();
		memory.destroy(boiler);
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null);
	}

	private void compressBc7Images() {
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		int imageIndex = -1;
		for (Image entry : images) {
			imageIndex += 1;
			if (entry.compression != Vk2dImageCompression.BC7) continue;

			int rememberImageIndex = imageIndex;
			threadPool.submit(() -> bcImageData[rememberImageIndex] = Bc7Compressor.compressBc7(entry.image));
		}
		threadPool.close();

		imageIndex = -1;
		for (Image entry : images) {
			imageIndex += 1;
			if (entry.compression != Vk2dImageCompression.BC7) continue;

			if (bcImageData[imageIndex] == null) throw new RuntimeException("BC7 compression apparently failed");
		}
	}

	public void write(OutputStream rawOutput) throws IOException {
		DataOutputStream output = new DataOutputStream(rawOutput);
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
			}
		}

		this.bcImageData = new byte[images.size()][];
		compressBc1Images();
		compressBc7Images();

		int index = 0;
		for (Image entry : images) {
			if (entry.compression == Vk2dImageCompression.NONE) writeUncompressedImage(output, entry.image);
			else if (entry.compression == Vk2dImageCompression.BC1 || entry.compression == Vk2dImageCompression.BC7) {
				output.write(bcImageData[index]);
			} else throw new UnsupportedOperationException("Unexpected compression " + entry.compression);
			index += 1;
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

		output.flush();
		if (ftLibrary != 0L) {
			assertFtSuccess(FT_Done_FreeType(ftLibrary), "Done_FreeType", "Vk2dResourceWriter");
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

	private record Image(BufferedImage image, Vk2dImageCompression compression, boolean pixelated) {}

	private record FakeImage(int width, int height, int[] data) {}

	private record Font(FT_Face ftFace, FontCurve[] curves, FontGlyph[] glyphs) {}

	private record FontCurve(int packedX, int packedY) {}

	private record FontGlyph(int curveIndex, int numCurves, float minX, float minY, float maxX, float maxY) {}
}
