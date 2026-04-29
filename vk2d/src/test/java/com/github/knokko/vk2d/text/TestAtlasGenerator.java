package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.vulkan.VK10.*;

public class TestAtlasGenerator {

	@Test
	public void checkWhetherTheAtlasesMakeSense() {
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "TestGlyphs", 1
		).validation().forbidValidationErrors().build();

		var config = new Vk2dConfig();
		config.simpleText = true;
		var instance = new Vk2dInstance(boiler, config);

		var writer = new Vk2dResourceWriter();
		int fontDataThaana = writer.addFontBlob(TestFontCollection.thaanaFont());
		int fontThaana = writer.addFont(fontDataThaana, 0);
		writer.addFallbackAtlas(fontThaana, 8, 30f, 0.1f);

		int fontDataMyriad = writer.addFontBlob(TestFontCollection.myriadFont());
		int fontMyriad = writer.addFont(fontDataMyriad, 0);
		writer.addAtlas(
				fontMyriad, 16, 10f, 0.1f, 2f, 20f,
				0f, 2f, "tiny"
		);
		writer.addFallbackAtlas(fontMyriad, 16, 50f, 0.5f);

		var bundle = writer.directlyCreateBundle(instance, null);

		var thaanaAtlas = bundle.getFont(fontThaana).chooseAtlas(10f, 1f, 0);

		// glyph 81 is for the 't' character, which is contained in "tiny"
		var smallMyriadAtlas = bundle.getFont(fontMyriad).chooseAtlas(10f, 0f, 81);
		var largeMyriadAtlas = bundle.getFont(fontMyriad).chooseAtlas(100f, 5f, 0);

		var readbackCombiner = new MemoryCombiner(boiler, "ReadbackMemory");
		var readBackBuffer = readbackCombiner.addMappedBuffer(
				2L * largeMyriadAtlas.image.width * largeMyriadAtlas.image.height,
				16L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
		);
		var readbackMemory = readbackCombiner.build(true);

		SdfAtlas[] atlases = { thaanaAtlas, smallMyriadAtlas, largeMyriadAtlas };
		assertNotSame(atlases[0], atlases[1]);
		assertNotSame(atlases[1], atlases[2]);

		for (var atlas : atlases) {
			SingleTimeCommands.submit(boiler, "CopyAtlas", recorder -> {
				recorder.transitionLayout(
						atlas.image, ResourceUsage.shaderRead(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
						ResourceUsage.TRANSFER_SOURCE
				);
				recorder.copyImageToBuffer(atlas.image, readBackBuffer);
				recorder.bufferBarrier(readBackBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.HOST_READ);
			}).destroy();

			int width = atlas.image.width;
			int height = atlas.image.height;
			int[] glyphs;
			if (atlas == thaanaAtlas) glyphs = new int[bundle.getFont(fontThaana).glyphExtents.capacity()];
			else if (atlas == smallMyriadAtlas) glyphs = new int[] { 81, 76, 92, 87 };
			else glyphs = new int[bundle.getFont(fontMyriad).glyphExtents.capacity()];

			if (atlas != smallMyriadAtlas) {
				for (int glyph = 0; glyph < glyphs.length; glyph++) {
					glyphs[glyph] = glyph;
				}
			}

			int coveredPixels = 0;
			for (int glyph : glyphs) {
				int minX = atlas.getMinX(glyph);
				if (minX == -1) continue;
				int minY = atlas.getMinY(glyph);
				int boundX = minX + atlas.getWidth(glyph);
				int boundY = minY + atlas.getHeight(glyph);

				// Check that there is no overlap
				if (atlas != thaanaAtlas) {
					for (int otherGlyph : glyphs) {
						if (glyph == otherGlyph) continue;

						int otherMinX = atlas.getMinX(otherGlyph);
						if (otherMinX == -1) continue;

						// Check that there is padding
						otherMinX -= 1;
						int otherMinY = atlas.getMinY(otherGlyph) - 1;
						int otherBoundX = otherMinX + 2 + atlas.getWidth(otherGlyph);
						int otherBoundY = otherMinY + 2 + atlas.getHeight(otherGlyph);
						if (minX < otherBoundX && minY < otherBoundY && boundX > otherMinX && boundY > otherMinY) {
							throw new RuntimeException("Glyph " + glyph + " overlaps with " + otherGlyph);
						}
					}
				}

				coveredPixels += atlas.getWidth(glyph) * atlas.getHeight(glyph);

				// Check that all the borders are *outside* the glyph
				for (int x = minX; x < boundX; x++) {
					if (atlas.bitsPerPixel == 16) {
						float rawDistance1 = (float) readBackBuffer.shortBuffer().get(x + minY * width) / Short.MAX_VALUE;
						float rawDistance2 = (float) readBackBuffer.shortBuffer().get(x + (boundY - 1) * width) / Short.MAX_VALUE;
						assertTrue(rawDistance1 < -0.4f, () -> "Expected " + rawDistance1 + " to be below -0.4");
						assertTrue(rawDistance2 < -0.4f, () -> "Expected " + rawDistance2 + " to be below -0.4");
					} else {
						float rawDistance1 = (float) readBackBuffer.byteBuffer().get(x + minY * width) / Byte.MAX_VALUE;
						float rawDistance2 = (float) readBackBuffer.byteBuffer().get(x + (boundY - 1) * width) / Byte.MAX_VALUE;
						assertTrue(rawDistance1 < -0.4f, () -> "Expected " + rawDistance1 + " to be below -0.4");
						assertTrue(rawDistance2 < -0.4f, () -> "Expected " + rawDistance2 + " to be below -0.4");
					}
				}

				for (int y = minY; y < boundY; y++) {
					if (atlas.bitsPerPixel == 16) {
						float rawDistance1 = (float) readBackBuffer.shortBuffer().get(minX + y * width) / Short.MAX_VALUE;
						float rawDistance2 = (float) readBackBuffer.shortBuffer().get(boundX - 1 + y * width) / Short.MAX_VALUE;
						assertTrue(rawDistance1 < -0.4f, () -> "Expected " + rawDistance1 + " to be below -0.4");
						assertTrue(rawDistance2 < -0.4f, () -> "Expected " + rawDistance2 + " to be below -0.4");
					} else {
						float rawDistance1 = (float) readBackBuffer.byteBuffer().get(minX + minY * width) / Byte.MAX_VALUE;
						float rawDistance2 = (float) readBackBuffer.byteBuffer().get(boundX - 1 + y * width) / Byte.MAX_VALUE;
						assertTrue(rawDistance1 < -0.4f, () -> "Expected " + rawDistance1 + " to be below -0.4");
						assertTrue(rawDistance2 < -0.4f, () -> "Expected " + rawDistance2 + " to be below -0.4");
					}
				}
			}

			float totalPixels = width * height;
			assertTrue(coveredPixels > 0.6f * totalPixels, "Expected at least 60% of pixels to be used");

			int insidePixels = 0;
			if (atlas.bitsPerPixel == 16) {
				var buffer = readBackBuffer.shortBuffer().limit(width * height);
				while (buffer.hasRemaining()) {
					if (buffer.get() > 0) insidePixels += 1;
				}
			} else {
				var buffer = readBackBuffer.byteBuffer().limit(width * height);
				while (buffer.hasRemaining()) {
					if (buffer.get() > 0) insidePixels += 1;
				}
			}

			assertTrue(
					insidePixels > 0.03f * totalPixels,
					"Expected at least 3% of pixels to be inside glyphs"
			);
			assertTrue(
					insidePixels < 0.3f * totalPixels,
					"Expected at most 30% of pixels to be inside glyphs"
			);
		}

		readbackMemory.destroy(boiler);
		bundle.destroy(boiler);
		instance.destroy();
		boiler.destroyInitialObjects();
	}
}
