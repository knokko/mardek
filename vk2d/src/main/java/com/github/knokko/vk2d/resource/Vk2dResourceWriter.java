package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.builders.BoilerBuilder;
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

import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
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
		int numBc1Images = 0;
		int numBc4Images = 0;
		for (Image image : images) {
			if (image.compression == Vk2dImageCompression.BC1) numBc1Images += 1;
			if (image.compression == Vk2dImageCompression.BC4) numBc4Images += 1;
		}
		if (numBc1Images == 0 && numBc4Images == 0) return;

		BoilerInstance boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "Vk2dBc1/4Writer", 1
		).validation().forbidValidationErrors().doNotUseVma().defaultTimeout(100_000_000_000L).build();

		if (numBc1Images > 0) {
			var bc1Images = new BufferedImage[numBc1Images];
			int nextIndex = 0;
			for (var image : images) {
				if (image.compression == Vk2dImageCompression.BC1) {
					bc1Images[nextIndex++] = image.image;
				}
			}

			Bc1Compressor.compressBufferedImages(bc1Images, boiler, compressedBuffers -> {
				int index = 0;
				for (var image : images) {
					if (image.compression == Vk2dImageCompression.BC1) {
						image.data = new byte[compressedBuffers[index].remaining()];
						compressedBuffers[index].get(image.data);
						index += 1;
					}
				}
			});
		}

		if (numBc4Images > 0) {
			var imageData = new ByteBuffer[numBc4Images];
			var widths = new int[numBc4Images];
			var heights = new int[numBc4Images];

			int nextIndex = 0;
			for (Image image : images) {
				if (image.compression != Vk2dImageCompression.BC4) continue;

				var data = ByteBuffer.allocate(image.image.getWidth() * image.image.getHeight());
				for (int y = 0; y < image.image.getHeight(); y++) {
					for (int x = 0; x < image.image.getWidth(); x++) {
						Color color = new Color(image.image.getRGB(x, y), true);

						byte greyscale = switch (image.channel) {
							case Vk2dGreyscaleChannel.RGB -> (byte) ((color.getRed() + color.getGreen() + color.getBlue()) / 3);
							case Vk2dGreyscaleChannel.ALPHA -> (byte) color.getAlpha();
							case Vk2dGreyscaleChannel.RED -> (byte) color.getRed();
						};
						data.put(greyscale);
					}
				}
				data.flip();
				imageData[nextIndex] = data;
				widths[nextIndex] = image.image.getWidth();
				heights[nextIndex] = image.image.getHeight();
				nextIndex += 1;
			}

			Bc4Compressor.compressGreyscaleImageData(imageData, widths, heights, false, boiler, compressedBuffers -> {
				int index = 0;
				for (var image : images) {
					if (image.compression == Vk2dImageCompression.BC4) {
						image.data = new byte[compressedBuffers[index].remaining()];
						compressedBuffers[index].get(image.data);
						index += 1;
					}
				}
			});
		}
	}

	private void compressBc7Images() {
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		for (Image entry : images) {
			if (entry.compression != Vk2dImageCompression.BC7) continue;

			if (entry.data == null) {
				threadPool.submit(() -> {
					entry.data = Bc7Compressor.compressBufferedImage(Bc7Compressor.FLAGS_DEFAULT_SLOWEST, entry.image);
				});
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
