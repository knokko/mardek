package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dImageBatch;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import static com.github.knokko.vk2d.ImageBenchmarkResourceWriter.FILE;
import static java.lang.Math.min;

public class ChaosImageBenchmark extends Vk2dWindow {

	public ChaosImageBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(FILE.toPath());
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.image = true;
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();

		Random rng = new Random();
		int numImages = 10_000;
		int minWidth = min(10, swapchainImage.getWidth());
		int minHeight = min(10, swapchainImage.getHeight());

		Vk2dImageBatch batch1 = pipelines.image.addBatch(frame.swapchainStage, 2 * numImages, resources);
		for (int counter = 0; counter < numImages; counter++) {
			int minX = rng.nextInt(1 + swapchainImage.getWidth() - minWidth);
			int minY = rng.nextInt(1 + swapchainImage.getHeight() - minHeight);
			int boundWidth = swapchainImage.getWidth() - minX;
			int boundHeight = swapchainImage.getHeight() - minY;
			batch1.simple(
					minX, minY, minX + rng.nextInt(boundWidth), minY + rng.nextInt(boundHeight),
					rng.nextInt(resources.numImages)
			);
		}
	}

	public static void main(String[] args) {
		bootstrap("ChaosImageBenchmark", 1, Vk2dValidationMode.NONE, ChaosImageBenchmark::new);
	}
}
