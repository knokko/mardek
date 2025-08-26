package com.github.knokko.vk2d.pipeline;

import com.github.knokko.vk2d.batch.Vk2dKimBatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.github.knokko.vk2d.pipeline.PipelineTester.staticPipelines;

public class TestKim3Pipeline {

	@BeforeAll
	public static void setupShared() {
		PipelineTester.staticSetup(config -> config.kim3 = true);
	}

	@Test
	public void testSimple() {
		PipelineTester.runTest(
				"kim-pipeline-simple",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
			Vk2dKimBatch batch = staticPipelines.kim3.addBatch(tester.stage(), 2, tester.bundle());
			batch.simple(3, 20, 60, 45, 1);
		});
	}

	@Test
	public void testScaled() {
		PipelineTester.runTest(
				"kim-pipeline-scale",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
			Vk2dKimBatch batch = staticPipelines.kim3.addBatch(tester.stage(), 2, tester.bundle());
			batch.simple(5, 3, 2.5f, 1);
		});
	}

	@AfterAll
	public static void cleanUpShared() {
		PipelineTester.staticTearDown();
	}
}
