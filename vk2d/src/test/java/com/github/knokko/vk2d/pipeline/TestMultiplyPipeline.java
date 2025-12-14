package com.github.knokko.vk2d.pipeline;

import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import com.github.knokko.vk2d.batch.Vk2dMultiplyBatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.knokko.boiler.utilities.ColorPacker.*;
import static com.github.knokko.vk2d.pipeline.PipelineTester.staticPipelines;

public class TestMultiplyPipeline {

	@BeforeAll
	public static void setupShared() {
		PipelineTester.staticSetup(config -> {
			config.color = true;
			config.multiply = true;
		});
	}

	@Test
	public void testFill() {
		PipelineTester.runTest("multiply-pipeline-fill", null, 100, 50, tester -> {
			Vk2dColorBatch colorBatch = staticPipelines.color.addBatch(tester.stage(), 2);
			colorBatch.fill(20, 30, 80, 45, srgbToLinear(rgba(100, 150, 60, 200)));

			Vk2dMultiplyBatch multiplyBatch = staticPipelines.multiply.addBatch(tester.stage(), 4);
			multiplyBatch.fill(10, 32, 70, 37, srgbToLinear(rgb(0.7f, 0.2f, 0.5f)));
			multiplyBatch.fill(10, 38, 70, 42, rgba(0.7f, 0.2f, 0.5f, 0.1f));
		});
	}

	@Test
	public void testGradient() {
		PipelineTester.runTest("multiply-pipeline-gradient", null, 100, 50, tester -> {
			Vk2dColorBatch colorBatch = staticPipelines.color.addBatch(tester.stage(), 3);
			colorBatch.fill(4, 0, 99, 49, srgbToLinear(rgb(200, 100, 50)));

			Vk2dMultiplyBatch multiplyBatch = staticPipelines.multiply.addBatch(tester.stage(), 23);
			multiplyBatch.gradient(
					2, 14, 71, 45,
					srgbToLinear(rgb(80, 23, 71)),
					srgbToLinear(rgb(80, 255, 171)),
					srgbToLinear(rgb(190, 25, 17))
			);
		});
	}

	@Test
	public void testFillUnaligned() {
		PipelineTester.runTest("multiply-pipeline-unaligned", null, 100, 50, tester -> {
			Vk2dColorBatch colorBatch = staticPipelines.color.addBatch(tester.stage(), 1);
			colorBatch.fill(0, 0, 99, 49, srgbToLinear(rgb(200, 100, 50)));

			Vk2dMultiplyBatch multiplyBatch = staticPipelines.multiply.addBatch(tester.stage(), 1);
			multiplyBatch.fillUnaligned(
					20, 30, 80, 40,
					90, 35, 45, 10,
					srgbToLinear(rgb(0.8f, 0.3f, 0.4f))
			);
		});
	}

	@Test
	public void testGradientUnaligned() {
		PipelineTester.runTest("multiply-pipeline-gradient-unaligned", null, 100, 50, tester -> {
			Vk2dColorBatch colorBatch = staticPipelines.color.addBatch(tester.stage(), 0);
			colorBatch.fill(0, 0, 99, 49, srgbToLinear(rgb(200, 100, 50)));

			Vk2dMultiplyBatch multiplyBatch = staticPipelines.multiply.addBatch(tester.stage(), 0);
			multiplyBatch.gradientUnaligned(
					20, 30, srgbToLinear(rgb(0.4f, 0.2f, 0.5f)),
					80, 40, srgbToLinear(rgb(0.4f, 1f, 0.7f)),
					90, 35, srgbToLinear(rgb(1f, 0.8f, 0)),
					45, 10, srgbToLinear(rgb(0.9f, 0.3f, 0.1f))
			);
		});
	}

	@AfterAll
	public static void cleanUpShared() {
		PipelineTester.staticTearDown();
	}
}
