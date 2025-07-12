package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import static com.github.knokko.vk2d.ImageBenchmarkResourceWriter.FILE;

public class StructuredImageBenchmark extends Vk2dWindow {

	private Vk2dImagePipeline imagePipeline;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public StructuredImageBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(FILE.toPath());
	}

	@Override
	protected void createResources(BoilerInstance boiler, MemoryCombiner combiner) {
		super.createResources(boiler, combiner);
		this.imagePipeline = new Vk2dImagePipeline(pipelineContext, shared);
	}

	@Override
	protected void renderFrame(Vk2dFrame frame, CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler) {
		long currentTime = System.nanoTime();
		if (currentTime - referenceTime > 1000_000_000L) {
			System.out.println("FPS is " + fps);
			fps = 0;
			referenceTime = currentTime;
		}
		fps += 1;

		Random rng = new Random();
		int numRounds = 1;
		int scale = 1;
		Vk2dImageBatch batch1 = frame.addBatch(imagePipeline, 5_000);
		for (int round = 0; round < numRounds; round++) {
			for (int y = 0; y < swapchainImage.height(); y += 16 * scale) {
				for (int x = 0; x < swapchainImage.width(); x += 16 * scale) {
					batch1.simple(
							x, y, x + 16 * scale - 1, y + 16 * scale - 1,
							resources.getImageDescriptor(rng.nextInt(resources.numImages))
					);
				}
			}
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		imagePipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("StructuredImageBenchmark", 1, Vk2dValidationMode.NONE, StructuredImageBenchmark::new);
	}
}
