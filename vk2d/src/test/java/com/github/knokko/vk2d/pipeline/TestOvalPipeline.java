package com.github.knokko.vk2d.pipeline;

import com.github.knokko.vk2d.batch.Vk2dOvalBatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear;
import static com.github.knokko.vk2d.pipeline.PipelineTester.staticPipelines;

public class TestOvalPipeline {

	@BeforeAll
	public static void setupShared() {
		PipelineTester.staticSetup(config -> config.oval = true);
	}

	@Test
	public void testSimpleAliased() {
		PipelineTester.runTest("oval-pipeline-simple-aliased", null, 50, 50, tester -> {
			Vk2dOvalBatch batch = staticPipelines.oval.addBatch(tester.stage(), tester.perFrameDescriptorSet(), 2);
			batch.simpleAliased(12, 3, 40, 28, srgbToLinear(rgb(200, 50, 100)));
		});
	}

	@Test
	public void testAliased() {
		PipelineTester.runTest("oval-pipeline-aliased", null, 50, 50, tester -> {
			Vk2dOvalBatch batch = staticPipelines.oval.addBatch(tester.stage(), tester.perFrameDescriptorSet(), 2);
			batch.aliased(
					12, 3, 40, 28,
					20.5f, 20, 15, 10.1f,
					srgbToLinear(rgb(200, 50, 100))
			);
		});
	}

	@Test
	public void testSimpleAntiAliased() {
		PipelineTester.runTest("oval-pipeline-simple-anti-aliased", null, 50, 50, tester -> {
			Vk2dOvalBatch batch = staticPipelines.oval.addBatch(tester.stage(), tester.perFrameDescriptorSet(), 2);
			batch.simpleAntiAliased(12, 3, 40, 28, 0.5f, srgbToLinear(rgb(200, 150, 50)));
		});
	}

	@Test
	public void testAntiAliased() {
		PipelineTester.runTest("oval-pipeline-anti-aliased", null, 50, 50, tester -> {
			Vk2dOvalBatch batch = staticPipelines.oval.addBatch(tester.stage(), tester.perFrameDescriptorSet(), 2);
			batch.antiAliased(
					12, 3, 40, 28,
					20.5f, 20, 15, 10.1f,
					0.1f, srgbToLinear(rgb(200, 250, 50))
			);
		});
	}

	@Test
	public void testComplex() {
		PipelineTester.runTest("oval-pipeline-complex", null, 50, 50, tester -> {
			Vk2dOvalBatch batch = staticPipelines.oval.addBatch(tester.stage(), tester.perFrameDescriptorSet(), 2);
			batch.complex(
					10, 5, 40, 35,
					10f, 20, 20f, 10.1f,
					rgb(250, 0, 0), rgb(0, 250, 0), rgb(0, 0, 250),
					rgb(250, 250, 0), rgb(0, 250, 250),
					0.5f, 1f, 1.2f, 1.5f
			);
		});
	}

	@AfterAll
	public static void cleanUpShared() {
		PipelineTester.staticTearDown();
	}
}
