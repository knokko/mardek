package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.vk2d.resource.Vk2dFont;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;
import com.github.knokko.vk2d.resource.Vk2dResourceLoader;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

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

		MemoryCombiner memoryCombiner = new MemoryCombiner(boiler, "TestMemory");
		DescriptorCombiner descriptorCombiner = new DescriptorCombiner(boiler);
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
		// TODO Update descriptor set
		Vk2dResourceBundle fontBundle = loader.finish(boiler, shared);
		Vk2dFont font = fontBundle.getFont(0);

		commands.submit("Scratch", recorder -> {
			vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, sharedText.scratchPipeline);
			recorder.bindComputeDescriptors(sharedText.scratchPipelineLayout, descriptorSet);

			int glyph = 4; // In this font, glyph 4 is uppercase A

			ByteBuffer pushConstants = recorder.stack.calloc(28);
			pushConstants.putInt(0, 0); // intersectionDataOffset
			pushConstants.putInt(4, 0); // intersectionInfoOffset
			pushConstants.putInt(8, font.getFirstCurve(glyph));
			pushConstants.putInt(12, font.getNumCurves(glyph));
			pushConstants.putInt(16, 100); // pixelHeight
			pushConstants.putFloat(20, font.getGlyphMinY(glyph));
			pushConstants.putFloat(24, font.getGlyphMaxY(glyph));
			vkCmdPushConstants(
					recorder.commandBuffer, sharedText.scratchPipelineLayout,
					VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants
			);
			// TODO dispatch
		});
		commands.destroy();
		// TODO Check result
		vkDestroyDescriptorPool(boiler.vkDevice(), vkDescriptorPool, null);
		memory.destroy(boiler);
		shared.destroy(boiler);
		sharedText.destroy(boiler);
		boiler.destroyInitialObjects();
	}
}
