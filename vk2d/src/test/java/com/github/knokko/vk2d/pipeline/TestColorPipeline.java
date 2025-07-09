package com.github.knokko.vk2d.pipeline;

import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import org.junit.jupiter.api.*;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear;
import static com.github.knokko.vk2d.pipeline.PipelineTester.staticPipelines;

public class TestColorPipeline {

	@BeforeAll
	public static void setupShared() {
		PipelineTester.staticSetup(config -> config.color = true);
	}

	@Test
	public void testFill() {
		PipelineTester.runTest("color-pipeline-fill", null, 100, 50, tester -> {
			Vk2dColorBatch batch = staticPipelines.color.addBatch(tester.stage(), 2);
			batch.fill(20, 30, 80, 45, srgbToLinear(rgb(80, 23, 71)));
		});
	}

	@Test
	public void testGradient() {
		PipelineTester.runTest("color-pipeline-gradient", null, 100, 50, tester -> {
			Vk2dColorBatch batch = staticPipelines.color.addBatch(tester.stage(), 23);
			batch.gradient(
					2, 14, 71, 45,
					srgbToLinear(rgb(80, 23, 71)),
					srgbToLinear(rgb(80, 255, 171)),
					srgbToLinear(rgb(190, 25, 17))
			);
		});
	}

	@Test
	public void testFillUnaligned() {
		PipelineTester.runTest("color-pipeline-unaligned", null, 100, 50, tester -> {
			Vk2dColorBatch batch = staticPipelines.color.addBatch(tester.stage(), 1);
			batch.fillUnaligned(
					20, 30, 80, 40,
					90, 35, 45, 10,
					srgbToLinear(rgb(80, 23, 71))
			);
		});
	}

	@Test
	public void testGradientUnaligned() {
		PipelineTester.runTest("color-pipeline-gradient-unaligned", null, 100, 50, tester -> {
			Vk2dColorBatch batch = staticPipelines.color.addBatch(tester.stage(), 0);
			batch.gradientUnaligned(
					20, 30, srgbToLinear(rgb(80, 23, 71)),
					80, 40, srgbToLinear(rgb(80, 255, 171)),
					90, 35, srgbToLinear(rgb(200, 200, 0)),
					45, 10, srgbToLinear(rgb(190, 25, 17))
			);
		});
	}

	@AfterAll
	public static void cleanUpShared() {
		PipelineTester.staticTearDown();
	}
}
