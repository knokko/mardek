package com.github.knokko.vk2d.pipeline;

import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.github.knokko.boiler.utilities.ColorPacker.*;
import static com.github.knokko.vk2d.pipeline.PipelineTester.staticPipelines;

public class TestImagePipeline {

	@BeforeAll
	public static void setupShared() {
		PipelineTester.staticSetup(config -> config.image = true);
	}

	@Test
	public void testSimple() {
		PipelineTester.runTest(
				"image-pipeline-simple",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
			Vk2dImageBatch batch = staticPipelines.image.addBatch(tester.stage(), 2, tester.bundle());
			batch.simple(2.7f, 20.1f, 60.2f, 45, 0);
		});
	}

	@Test
	public void testSimpleScale() {
		PipelineTester.runTest(
				"image-pipeline-simple-scale",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
			Vk2dImageBatch batch = staticPipelines.image.addBatch(tester.stage(), 2, tester.bundle());
			batch.simpleScale(5, 3.7f, 2.5f, 1);
		});
	}

	@Test
	public void testColored() {
		PipelineTester.runTest(
				"image-pipeline-colored",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
					Vk2dImageBatch batch = staticPipelines.image.addBatch(tester.stage(), 2, tester.bundle());
			batch.colored(
					2.7f, 20.1f, 60.2f, 45, 0,
					rgba(0.2f, 0f, 0.2f, 0f), rgb(0.8f, 0.8f, 0.8f)
			);
		});
	}

	@Test
	public void testColoredScale() {
		PipelineTester.runTest(
				"image-pipeline-colored-scale",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
			Vk2dImageBatch batch = staticPipelines.image.addBatch(tester.stage(), 2, tester.bundle());
			batch.coloredScale(
					5, 3.7f, 2.5f, 1,
					rgba(0f, 0f, 0.5f, 0f), rgb(0.5f, 0.5f, 0.5f)
			);
		});
	}

	@Test
	public void testFillWithoutDistortion() {
		PipelineTester.runTest(
				"image-pipeline-fill",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
				Vk2dImageBatch batch = staticPipelines.image.addBatch(tester.stage(), 2, tester.bundle());
				batch.fillWithoutDistortion(5, 3.7f, 68.5f, 47f, 80);
			});
	}

	@Test
	public void testRotated() {
		PipelineTester.runTest(
				"image-pipeline-rotated",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
			Vk2dImageBatch batch = staticPipelines.image.addBatch(tester.stage(), 2, tester.bundle());
			batch.rotated(
					35, 23.7f, 90f, 1.5f, 2,
					rgba(0f, 0.5f, 0.5f, 0f), rgb(0.5f, 0.5f, 0.5f)
			);
		});
	}

	@Test
	public void testTransformed() {
		PipelineTester.runTest(
				"image-pipeline-transformed",
				new File("../image-benchmark-resources.bin"),
				70, 50, tester -> {
			Vk2dImageBatch batch = staticPipelines.image.addBatch(tester.stage(), 2, tester.bundle());
			batch.transformed(
					5f, 42.5f, 65.1f, 40f, 58.8f, 10.4f, 10f, 30f,
					50, rgba(0.2f, 0.2f, 0f, 0f), rgb(0.8f, 0.8f, 0.8f)
			);
		});
	}

	@AfterAll
	public static void cleanUpShared() {
		PipelineTester.staticTearDown();
	}
}
