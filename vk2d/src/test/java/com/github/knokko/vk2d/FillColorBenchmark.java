package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;

import java.util.Random;

import static java.lang.Math.min;

public class FillColorBenchmark extends Vk2dWindow {

	public FillColorBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.color = true;
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();

		Random rng = new Random();
		int numRectangles = 10_000;
		int minWidth = min(10, swapchainImage.width());
		int minHeight = min(10, swapchainImage.height());

		Vk2dColorBatch batch1 = pipelines.color.addBatch(frame.swapchainStage, 2 * numRectangles);
		for (int counter = 0; counter < numRectangles; counter++) {
			int minX = rng.nextInt(1 + swapchainImage.width() - minWidth);
			int minY = rng.nextInt(1 + swapchainImage.height() - minHeight);
			int boundWidth = swapchainImage.width() - minX;
			int boundHeight = swapchainImage.height() - minY;
			batch1.fill(
					minX, minY, minX + rng.nextInt(boundWidth),
					minY + rng.nextInt(boundHeight), rng.nextInt()
			);
		}
	}

	public static void main(String[] args) {
		bootstrap("FillColorBenchmark", 1, Vk2dValidationMode.NONE, FillColorBenchmark::new);
	}
}
