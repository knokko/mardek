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
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Outline_Funcs;
import org.lwjgl.util.freetype.FT_Vector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static java.lang.Math.max;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.vulkan.VK10.*;

public class Vk2dResourceWriter {

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

	public int addFont(FT_Face font) {
		List<FontCurve> curves = new ArrayList<>();
		List<FontGlyph> glyphs = new ArrayList<>();
		try (MemoryStack stack = stackPush()) {
			assertFtSuccess(FT_Load_Char(font, 'A', FT_LOAD_NO_SCALE), "Load_Char", "A");
			float heightA = Objects.requireNonNull(font.glyph()).metrics().height();

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
				curves.add(new FontCurve(
						position.x() / heightA, position.y() / heightA,
						0.5f * (position.x() + to.x()) / heightA,
						0.5f * (position.y() + to.y()) / heightA,
						to.x() / heightA, to.y() / heightA
				));
				position.x(to.x());
				position.y(to.y());
				return 0;
			});
			outlineFunctions.conic_to((long rawControl, long rawTo, long userData) -> {
				@SuppressWarnings("resource") var control = FT_Vector.create(rawControl);
				@SuppressWarnings("resource") var to = FT_Vector.create(rawTo);
				curves.add(new FontCurve(
						position.x() / heightA, position.y() / heightA,
						control.x() / heightA, control.y() / heightA,
						to.x() / heightA, to.y() / heightA
				));
				position.x(to.x());
				position.y(to.y());
				return 0;
			});
			outlineFunctions.cubic_to((long control1, long control2, long to, long userData) -> 1);
			outlineFunctions.delta(0);
			outlineFunctions.shift(0);

			for (int glyph = 0; glyph < font.num_glyphs(); glyph++) {
				System.out.println("Glyph is " + glyph + " and #glyphs is " + font.num_glyphs());
				int curveIndex = curves.size();
				assertFtSuccess(FT_Load_Glyph(font, glyph, FT_LOAD_NO_SCALE), "Load_Glyph", "Vk2dResourceWriter");
				var outline = Objects.requireNonNull(font.glyph()).outline();
				assertFtSuccess(FT_Outline_Decompose(outline, outlineFunctions, 0L), "Outline_Decompose", "Vk2dResourceWriter");
				int numCurves = curves.size() - curveIndex;

				var metrics = Objects.requireNonNull(font.glyph()).metrics();
				glyphs.add(new FontGlyph(curveIndex, numCurves, metrics.width() / heightA, metrics.height() / heightA));
			}
		}

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
			output.writeInt(font.curves.size());
			output.writeInt(font.glyphs.size());
			for (FontGlyph glyph : font.glyphs) {
				output.writeFloat(glyph.width);
				output.writeFloat(glyph.height);
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
				output.writeFloat(curve.startX);
				output.writeFloat(curve.startY);
				output.writeFloat(curve.controlX);
				output.writeFloat(curve.controlY);
				output.writeFloat(curve.endX);
				output.writeFloat(curve.endY);
			}
			for (FontGlyph glyph : font.glyphs) {
				output.writeInt(glyph.curveIndex);
				output.writeInt(glyph.numCurves);
			}
		}

		output.flush();
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

	private record Font(FT_Face ftFace, List<FontCurve> curves, List<FontGlyph> glyphs) {}

	private record FontCurve(float startX, float startY, float controlX, float controlY, float endX, float endY) {}

	private record FontGlyph(int curveIndex, int numCurves, float width, float height) {}
}
