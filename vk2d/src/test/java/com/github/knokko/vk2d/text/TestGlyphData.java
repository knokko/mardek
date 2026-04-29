package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;

import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_font_get_nominal_glyph;
import static org.lwjgl.vulkan.VK10.*;

public class TestGlyphData {

	@Test
	public void testGlyphOutlinesOfI() {
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "TestGlyphs", 1
		).validation().forbidValidationErrors().build();

		var config = new Vk2dConfig();
		config.simpleText = true;
		var instance = new Vk2dInstance(boiler, config);

		var writer = new Vk2dResourceWriter();
		int fontDataThaana = writer.addFontBlob(TestFontCollection.thaanaFont());
		writer.addFont(fontDataThaana, 0);

		int fontDataMyriad = writer.addFontBlob(TestFontCollection.myriadFont());
		int fontMyriad = writer.addFont(fontDataMyriad, 0);

		var bundle = writer.directlyCreateBundle(instance, null);

		var font = bundle.getFont(fontMyriad);
		int glyphI;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			var pGlyphI = stack.callocInt(1);
			assertHbSuccess(hb_font_get_nominal_glyph(
					font.hbFont, 'i', pGlyphI
			), "font_get_nominal_glyph");
			glyphI = pGlyphI.get();
		}

		assertEquals(4, font.outlines.getNumLines(glyphI));
		assertEquals(8, font.outlines.getNumCurves(glyphI));

		var readbackCombiner = new MemoryCombiner(boiler, "ReadbackMemory");
		var readbackBuffer = readbackCombiner.addMappedBuffer(
				font.outlines.persistentBuffer.size, 4L, VK_BUFFER_USAGE_TRANSFER_DST_BIT
		);
		var readbackMemory = readbackCombiner.build(true);

		SingleTimeCommands.submit(boiler, "Readback", recorder -> {
			recorder.bufferBarrier(font.outlines.persistentBuffer, ResourceUsage.shaderRead(
					VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
			), ResourceUsage.TRANSFER_SOURCE);
			recorder.copyBuffer(font.outlines.persistentBuffer, readbackBuffer);
			recorder.bufferBarrier(readbackBuffer, ResourceUsage.TRANSFER_DEST, ResourceUsage.HOST_READ);
		}).destroy();

		var outlineBuffer = readbackBuffer.intBuffer();

		// Check first 2 lines
		outlineBuffer.position(font.outlines.getLinesOffset(glyphI));
		assertEquals(101 | (0), outlineBuffer.get());
		assertEquals(101 | (495 << 16), outlineBuffer.get());

		assertEquals(101 | (495 << 16), outlineBuffer.get());
		assertEquals(13 | (495 << 16), outlineBuffer.get());

		// Check first 2 curves
		outlineBuffer.position(font.outlines.getCurvesOffset(glyphI));
		assertEquals(112 | (641 << 16), outlineBuffer.get());
		assertEquals(112 | (666 << 16), outlineBuffer.get());
		assertEquals(96 | (683 << 16), outlineBuffer.get());

		assertEquals(96 | (683 << 16), outlineBuffer.get());
		assertEquals(81 | (699 << 16), outlineBuffer.get());
		assertEquals(56 | (699 << 16), outlineBuffer.get());

		readbackMemory.destroy(boiler);
		bundle.destroy(boiler);
		instance.destroy();
		boiler.destroyInitialObjects();
	}
}
