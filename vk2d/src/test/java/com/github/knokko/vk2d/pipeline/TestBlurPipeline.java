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

import static com.github.knokko.boiler.utilities.ColorPacker.*;
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
		)[0];
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

	@Test
	public void testDownscaledBlur() {
		PipelineTester.runTest(
				"blur-pipeline-downscaled", new File("../image-benchmark-resources.bin"),
				300, 350, tester -> {

					resources = createResources(vk2d, staticPipelines.blur, 350, 360, 3);
					setupBlurStages(tester, resources, 4);

					Vk2dImageBatch sourceImage = staticPipelines.image.addBatch(sourceStage, 2, tester.bundle());
					sourceImage.simpleScale(20, 30, 20f, 0);

					Vk2dColorBatch backgroundColor = staticPipelines.color.addBatch(tester.stage(), 2);
					backgroundColor.fill(0, 0, tester.stage().width, tester.stage().height, -1);

					Vk2dBlurBatch blurBatch = staticPipelines.blur.addBatch(
							tester.stage(), resources.framebuffer, resources.descriptors, 100, 150,
							100f + sourceStage.width / 2f, 150f + sourceStage.height / 2f
					);
					blurBatch.noColorTransform();
				});
		resources.destroy();
	}

	@Test
	public void testComplexBlur() {
		MemoryBlock[] memoryToDestroy = { null };
		long[] poolToDestroy = new long[1];
		PipelineTester.runTest(
				"blur-pipeline-complex", new File("../image-benchmark-resources.bin"),
				500, 500, tester -> {

					int sourceWidth = 500;
					int sourceHeight = 500;
					DescriptorCombiner combiner = new DescriptorCombiner(vk2d.boiler);
					Vk2dBlurPipeline.Descriptors[] allDescriptors = staticPipelines.blur.claimResources(
							3, vk2d, combiner
					);
					poolToDestroy[0] = combiner.build("TestBlurDescriptorPool");

					MemoryCombiner memory = new MemoryCombiner(vk2d.boiler, "TestBlurMemory");
					Vk2dBlurPipeline.Framebuffer framebuffer = staticPipelines.blur.createFramebuffer(
							memory, VK_FORMAT_R8G8B8A8_SRGB, sourceWidth, sourceHeight, sourceWidth, sourceHeight
					);
					memoryToDestroy[0] = memory.build(true);

					sourceStage = staticPipelines.blur.addSourceStage(tester.frame(), framebuffer, 0);
					staticPipelines.blur.addComputeStage(
							tester.frame(), allDescriptors[0], framebuffer,
							19, 50, 1
					);

					Vk2dRenderStage earlyStage = staticPipelines.blur.addSourceStage(tester.frame(), framebuffer, 2);
					staticPipelines.blur.addComputeStage(
							tester.frame(), allDescriptors[1], framebuffer,
							4, 50, 3
					);

					Vk2dRenderStage lateStage = staticPipelines.blur.addSourceStage(tester.frame(), framebuffer, 4);
					staticPipelines.blur.addComputeStage(
							tester.frame(), allDescriptors[2], framebuffer,
							9, 50, 5
					);

					Vk2dImageBatch sourceImage = staticPipelines.image.addBatch(sourceStage, 2, tester.bundle());
					sourceImage.simpleScale(2, 3, 30f, 0);

					Vk2dImageBatch earlyImage = staticPipelines.image.addBatch(earlyStage, 2, tester.bundle());
					staticPipelines.blur.addBatch(
							earlyStage, framebuffer, allDescriptors[0], 0.25f, 0.5f, 250.5f, 250
					).fixedColorTransform(rgba(0f, 0.25f, 0f, 0f), rgb(1f, 0.75f, 1f));
					earlyImage.simpleScale(250, 5, 15f, 1);

					Vk2dImageBatch lateImage = staticPipelines.image.addBatch(lateStage, 2, tester.bundle());
					staticPipelines.blur.addBatch(
							lateStage, framebuffer, allDescriptors[1], 0, 0, 500, 500
					).gradientColorTransform(
							0, -1,
							rgba(0.5f, 0f, 0f, 0f), rgb(0.5f, 0f, 0f),
							rgba(0f, 0.5f, 0f, 0f), rgb(0f, 0.5f, 0f),
							rgba(0f, 0f, 0.5f, 0f), rgb(0f, 0f, 0.5f)
					);
					lateImage.simpleScale(250, 230, 15f, 2);

					Vk2dColorBatch backgroundColor = staticPipelines.color.addBatch(tester.stage(), 2);
					backgroundColor.fill(0, 0, tester.stage().width, tester.stage().height, -1);

					staticPipelines.blur.addBatch(
							tester.stage(), framebuffer, allDescriptors[2],
							0, 0, 500f, 500
					).noColorTransform();
				});
		memoryToDestroy[0].destroy(boiler);
		vkDestroyDescriptorPool(boiler.vkDevice(), poolToDestroy[0], null);
	}

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
