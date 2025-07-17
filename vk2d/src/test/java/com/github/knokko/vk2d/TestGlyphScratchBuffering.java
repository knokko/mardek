package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.vk2d.resource.Vk2dFont;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import com.github.knokko.vk2d.resource.Vk2dResourceLoader;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class TestGlyphScratchBuffering {

	@Test
	public void testUpperCaseA() throws IOException {
		InputStream fontInput = Objects.requireNonNull(TextPlayground.class.getClassLoader().getResourceAsStream(
				"com/github/knokko/vk2d/fonts/thaana.ttf"
		));

		Vk2dResourceWriter writer = new Vk2dResourceWriter();
		writer.addFont(fontInput);

		ByteArrayOutputStream propagate = new ByteArrayOutputStream();
		writer.write(propagate);

		BoilerInstance boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TestUpperCaseA", 1
		).validation().forbidValidationErrors().build();

		Vk2dSharedText sharedText = new Vk2dSharedText(boiler);
		Vk2dShared shared = new Vk2dShared(boiler);

		int glyphHeight = 100;
		int glyph = 4; // In this font, glyph 4 is uppercase A

		MemoryCombiner memoryCombiner = new MemoryCombiner(boiler, "TestMemory");
		DescriptorCombiner descriptorCombiner = new DescriptorCombiner(boiler);

		long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		int usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
		MappedVkbBuffer intersectionBuffer = memoryCombiner.addMappedBuffer(15000L, alignment, usage);
		MappedVkbBuffer infoBuffer = memoryCombiner.addMappedBuffer(8L * glyphHeight, alignment, usage);
		Vk2dResourceLoader loader = new Vk2dResourceLoader(new ByteArrayInputStream(propagate.toByteArray()));
		loader.claimMemory(boiler, memoryCombiner);
		MemoryBlock memory = memoryCombiner.build(false);

		loader.prepareStaging();

		SingleTimeCommands commands = new SingleTimeCommands(boiler);
		commands.submit("Staging", recorder -> {
			loader.performStaging(recorder, shared, descriptorCombiner);
		}).awaitCompletion();
		long[] pDescriptorSet = descriptorCombiner.addMultiple(sharedText.scratchDescriptorLayout, 1);
		long vkDescriptorPool = descriptorCombiner.build("ScratchDescriptors");
		long descriptorSet = pDescriptorSet[0];

		Vk2dResourceBundle fontBundle = loader.finish(boiler, shared);
		Vk2dFont font = fontBundle.getFont(0);

		try (MemoryStack stack = stackPush()) {
			DescriptorUpdater updater = new DescriptorUpdater(stack, 3);
			updater.writeStorageBuffer(0, descriptorSet, 0, intersectionBuffer);
			updater.writeStorageBuffer(1, descriptorSet, 1, infoBuffer);
			updater.writeStorageBuffer(2, descriptorSet, 2, font.curveBuffer);
			updater.update(boiler);
		}

		commands.submit("Scratch", recorder -> {
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sharedText.scratchPipeline);
			recorder.bindComputeDescriptors(sharedText.scratchPipelineLayout, descriptorSet);

			ByteBuffer pushConstants = recorder.stack.calloc(28);
			pushConstants.putInt(0, 0); // intersectionDataOffset
			pushConstants.putInt(4, 0); // intersectionInfoOffset
			pushConstants.putInt(8, 2 * font.getFirstCurve(glyph));
			System.out.println("first curve is " + font.getFirstCurve(glyph) + " and num curves is " + font.getNumCurves(glyph));
			// 22 and 11
			pushConstants.putInt(12, font.getNumCurves(glyph));
			pushConstants.putInt(16, glyphHeight); // pixelHeight
			pushConstants.putFloat(20, font.getGlyphMinY(glyph));
			pushConstants.putFloat(24, font.getGlyphMaxY(glyph));
			vkCmdPushConstants(
					recorder.commandBuffer, sharedText.scratchPipelineLayout,
					VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
			);
			//noinspection SuspiciousNameCombination
			vkCmdDispatch(recorder.commandBuffer, glyphHeight, 1, 1);
		});
		commands.destroy();

		IntBuffer info = infoBuffer.intBuffer();
		FloatBuffer intersections = intersectionBuffer.floatBuffer();

		int numCurves = font.getNumCurves(glyph);
		for (int glyphY = 0; glyphY < glyphHeight; glyphY++) {
			int numIntersections = info.get();
			System.out.print("y " + glyphY + " has " + numIntersections + " intersections: ");
			for (int index = 0; index < numIntersections; index++) {
				System.out.print(" " + intersections.get(index + 2 * glyphY * numCurves));
			}
			System.out.println();
		}

		// TODO Check result
		vkDestroyDescriptorPool(boiler.vkDevice(), vkDescriptorPool, null);
		memory.destroy(boiler);
		shared.destroy(boiler);
		sharedText.destroy(boiler);
		boiler.destroyInitialObjects();
	}
}
