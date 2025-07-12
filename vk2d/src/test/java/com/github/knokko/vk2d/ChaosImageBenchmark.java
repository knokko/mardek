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
import static java.lang.Math.min;

public class ChaosImageBenchmark extends Vk2dWindow {

	private Vk2dImagePipeline imagePipeline;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public ChaosImageBenchmark(VkbWindow window) {
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
		int numImages = 10_000;
		int minWidth = min(10, swapchainImage.width());
		int minHeight = min(10, swapchainImage.height());

		Vk2dImageBatch batch1 = imagePipeline.addBatch(frame, 6 * numImages);
		for (int counter = 0; counter < numImages; counter++) {
			int minX = rng.nextInt(1 + swapchainImage.width() - minWidth);
			int minY = rng.nextInt(1 + swapchainImage.height() - minHeight);
			int boundWidth = swapchainImage.width() - minX;
			int boundHeight = swapchainImage.height() - minY;
			batch1.simple(
					minX, minY, minX + rng.nextInt(boundWidth), minY + rng.nextInt(boundHeight),
					resources.getImageDescriptor(rng.nextInt(resources.numImages))
			);
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		imagePipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("ChaosImageBenchmark", 1, Vk2dValidationMode.NONE, ChaosImageBenchmark::new);
	}
}
