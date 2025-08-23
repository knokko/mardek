package com.github.knokko.vk2d.pipeline;

import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryBlock;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.batch.Vk2dBlurBatch;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear;
import static com.github.knokko.vk2d.pipeline.PipelineTester.*;
import static org.lwjgl.vulkan.VK10.*;

public class TestBlurPipeline {

	@BeforeAll
	public static void setupShared() {
		PipelineTester.staticSetup(config -> {
			config.color = true;
			config.image = true;
			config.blur = true;
		});
	}

	private Resources resources;
	private Vk2dRenderStage sourceStage;

	private Resources createResources(Vk2dInstance vk2d, Vk2dBlurPipeline pipeline, int width, int height, int sizeFactor) {
		DescriptorCombiner combiner = new DescriptorCombiner(vk2d.boiler);
		Vk2dBlurPipeline.Descriptors descriptors = pipeline.claimResources(
				1, vk2d, combiner
		)[0]; // TODO Try multiple frames in flight
		long vkDescriptorPool = combiner.build("TestBlurDescriptorPool");

		MemoryCombiner memory = new MemoryCombiner(vk2d.boiler, "TestBlurMemory");
		Vk2dBlurPipeline.Framebuffer framebuffer = staticPipelines.blur.createFramebuffer(
				memory, VK_FORMAT_R8G8B8A8_SRGB, width, height,
				width / sizeFactor, height / sizeFactor
		);
		return new Resources(descriptors, vkDescriptorPool, framebuffer, memory.build(true));
	}

	private void setupBlurStages(PipelineTester tester, Resources resources, int filterSize) {
		sourceStage = staticPipelines.blur.addSourceStage(tester.frame(), resources.framebuffer, 0);
		staticPipelines.blur.addComputeStage(
				tester.frame(), resources.descriptors, resources.framebuffer,
				filterSize, 50, 1
		);
	}

	@Test
	public void testSimple() {
		PipelineTester.runTest("blur-pipeline-simple", null, 100, 50, tester -> {
			resources = createResources(vk2d, staticPipelines.blur, 60, 40, 1);
			setupBlurStages(tester, resources, 1);

			Vk2dColorBatch sourceColor = staticPipelines.color.addBatch(sourceStage, 2);
			sourceColor.fill(10, 20, 35, 30, srgbToLinear(rgb(80, 23, 71)));

			Vk2dColorBatch backgroundColor = staticPipelines.color.addBatch(tester.stage(), 2);
			backgroundColor.fill(0, 0, tester.stage().width, tester.stage().height, -1);

			Vk2dBlurBatch blurBatch = staticPipelines.blur.addBatch(
					tester.stage(), resources.framebuffer, resources.descriptors, 5, 5,
					5 + sourceStage.width, 5 + sourceStage.height
			);
			blurBatch.noColorTransform();
		});
		resources.destroy();
	}

	@Test
	public void testHeavyBlur() {
		PipelineTester.runTest(
				"blur-pipeline-heavy", new File("../image-benchmark-resources.bin"),
				800, 800, tester -> {

			resources = createResources(vk2d, staticPipelines.blur, 600, 400, 1);
			setupBlurStages(tester, resources, 19);

			Vk2dImageBatch sourceImage = staticPipelines.image.addBatch(sourceStage, 2, tester.bundle());
			sourceImage.simpleScale(20, 30, 20f, 0);

			Vk2dColorBatch backgroundColor = staticPipelines.color.addBatch(tester.stage(), 2);
			backgroundColor.fill(0, 0, tester.stage().width, tester.stage().height, -1);

			Vk2dBlurBatch blurBatch = staticPipelines.blur.addBatch(
					tester.stage(), resources.framebuffer, resources.descriptors, 100, 150,
					100 + sourceStage.width, 150 + sourceStage.height
			);
			blurBatch.noColorTransform();
		});
		resources.destroy();
	}

	// TODO Test downscaling, frames-in-flight, color transform, and multi-stage

	@AfterAll
	public static void cleanUpShared() {
		PipelineTester.staticTearDown();
	}

	private record Resources(
			Vk2dBlurPipeline.Descriptors descriptors,
			long vkDescriptorPool,
			Vk2dBlurPipeline.Framebuffer framebuffer,
			MemoryBlock memory
	) {
		void destroy() {
			memory.destroy(boiler);
			vkDestroyDescriptorPool(boiler.vkDevice(), vkDescriptorPool, null);
		}
	}
}
