package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.vk2d.resource.*;
import com.github.knokko.vk2d.text.Vk2dFont;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class TestGlyphScratchBufferingA {

	@Test
	public void testUpperCaseA() throws IOException {
		InputStream fontInput = Objects.requireNonNull(TestGlyphScratchBufferingA.class.getResourceAsStream(
				"fonts/thaana.ttf"
		));

		Vk2dResourceWriter writer = new Vk2dResourceWriter();
		writer.addFont(fontInput);

		ByteArrayOutputStream propagate = new ByteArrayOutputStream();
		writer.write(propagate, null);

		BoilerInstance boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TestUpperCaseA", 1
		).validation().forbidValidationErrors().doNotUseVma().defaultTimeout(10_000_000_000L).build();

		Vk2dConfig config = new Vk2dConfig();
		config.text = true;
		Vk2dInstance instance = new Vk2dInstance(boiler, config);

		int glyphHeight = 100;
		int glyph = 4; // In this font, glyph 4 is uppercase A

		MemoryCombiner memoryCombiner = new MemoryCombiner(boiler, "TestMemory");
		DescriptorCombiner descriptorCombiner = new DescriptorCombiner(boiler);

		long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		int usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
		MappedVkbBuffer scratchIntersectionBuffer = memoryCombiner.addMappedBuffer(4L * 11L * 4L * glyphHeight, alignment, usage);
		MappedVkbBuffer scratchInfoBuffer = memoryCombiner.addMappedBuffer(8L * glyphHeight, alignment, usage);
		MappedVkbBuffer intersectionBuffer = memoryCombiner.addMappedBuffer(2L * 11L * 4L * glyphHeight, alignment, usage);
		MappedVkbBuffer infoBuffer = memoryCombiner.addMappedBuffer(12L * glyphHeight, alignment, usage);
		MappedVkbBuffer nextOffsetBuffer = memoryCombiner.addMappedBuffer(4L, alignment, usage | VK_BUFFER_USAGE_TRANSFER_DST_BIT);
		MappedVkbBuffer nextIntersectionIndexBuffer = memoryCombiner.addMappedBuffer(4L, alignment, usage);
		Vk2dTextBuffer textBuffer = new Vk2dTextBuffer(
				scratchIntersectionBuffer, scratchInfoBuffer, intersectionBuffer, infoBuffer,
				nextOffsetBuffer, nextIntersectionIndexBuffer, instance, descriptorCombiner
		);
		Vk2dResourceLoader loader = new Vk2dResourceLoader(instance, new ByteArrayInputStream(propagate.toByteArray()));
		loader.claimMemory(memoryCombiner);
		MemoryBlock memory = memoryCombiner.build(false);

		loader.prepareStaging();

		SingleTimeCommands commands = new SingleTimeCommands(boiler);
		commands.submit("Staging", recorder ->
				loader.performStaging(recorder, descriptorCombiner)
		).awaitCompletion();
		long vkDescriptorPool = descriptorCombiner.build("ScratchDescriptors");

		Vk2dResourceBundle fontBundle = loader.finish();
		Vk2dFont font = fontBundle.getFont(0);

		textBuffer.initializeDescriptorSets();
		assertEquals(0, textBuffer.scratch(font, glyph, 0f, 123, glyphHeight, glyphHeight, true, 0f));
		commands.submit("GlyphTransfer", recorder -> {
			textBuffer.record(recorder);
			recorder.bulkBufferBarrier(
					ResourceUsage.computeBuffer(VK_ACCESS_SHADER_WRITE_BIT),
					ResourceUsage.HOST_READ, scratchInfoBuffer, scratchIntersectionBuffer
			);
		}).awaitCompletion();

		IntBuffer info = scratchInfoBuffer.intBuffer();
		FloatBuffer intersections = scratchIntersectionBuffer.floatBuffer();

		int numCurves = font.getNumCurves(glyph);
		for (int glyphY = 0; glyphY < glyphHeight; glyphY++) {
			int numIntersections = info.get();
			int maxIntersections = info.get();
			if (glyphY > 0 && glyphY < 99) {
				assertTrue(numIntersections == 2 || numIntersections == 4, "Expected " + numIntersections + " to be 2 or 4 for y " + glyphY);
			}
			assertEquals(maxIntersections, 2 * font.getNumCurves(glyph));
		}

		// y 0 has 4 intersections
		assertEquals(4, info.get(2));
		assertEquals(2 * numCurves, info.get(1));
		assertEquals(0f, intersections.get(), 0.01f);
		assertEquals(-1f, intersections.get(1));
		assertEquals(0.11f, intersections.get(2), 0.01f);
		assertEquals(-1f, intersections.get(3));
		assertEquals(0.88f, intersections.get(4), 0.01f);
		assertEquals(-1f, intersections.get(5));
		assertEquals(0.99f, intersections.get(6), 0.01f);
		assertEquals(-1f, intersections.get(7));

		// y 98 has 2 intersections
		assertEquals(2, info.get(196));
		assertEquals(2 * numCurves, info.get(197));
		assertEquals(0.45f, intersections.get(4 * 98 * numCurves), 0.01f);
		assertEquals(-1f, intersections.get(4 * 98 * numCurves + 1));
		assertEquals(0.54f, intersections.get(4 * 98 * numCurves + 2), 0.01f);
		assertEquals(-1f, intersections.get(4 * 98 * numCurves + 3));

		assertEquals(358, nextIntersectionIndexBuffer.intBuffer().get());
		info = infoBuffer.intBuffer();
		intersections = intersectionBuffer.floatBuffer();

		assertEquals(0, info.get(0));
		assertEquals(4, info.get(1));
		assertEquals(0, info.get(2));

		assertEquals(4, info.get(3));
		assertEquals(4, info.get(4));
		assertEquals(0, info.get(5));

		assertEquals(0f, intersections.get(0), 0.01f);
		assertEquals(0.11f, intersections.get(1), 0.01f);
		assertEquals(0.88f, intersections.get(2), 0.01f);
		assertEquals(0.99f, intersections.get(3), 0.01f);

		int nextIndex = 0;
		for (int glyphY = 0; glyphY < glyphHeight; glyphY++) {
			assertEquals(nextIndex, info.get());
			int numIntersections = info.get();
			assertEquals(0, info.get());

			nextIndex += numIntersections;
			if (glyphY > 0 && glyphY < 99) {
				assertTrue(numIntersections == 2 || numIntersections == 4, "Expected " + numIntersections + " to be 2 or 4 for y " + glyphY);
			}
			for (int counter = 0; counter < numIntersections; counter++) {
				float intersection = intersections.get();
				assertTrue(intersection > -0.01f && intersection < 1.01f, "Expected " + intersection + " to be in [0; 1]");
			}
		}

		commands.destroy();
		vkDestroyDescriptorPool(boiler.vkDevice(), vkDescriptorPool, null);
		memory.destroy(boiler);
		instance.destroy();
		boiler.destroyInitialObjects();
	}
}
