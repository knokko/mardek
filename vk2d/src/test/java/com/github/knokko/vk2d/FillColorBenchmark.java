package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dColorBatch;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

import java.util.Random;

import static java.lang.Math.min;

public class FillColorBenchmark extends Vk2dWindow {

	private Vk2dColorPipeline colorPipeline;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public FillColorBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected void createResources(BoilerInstance boiler, MemoryCombiner combiner) {
		super.createResources(boiler, combiner);
		this.colorPipeline = new Vk2dColorPipeline(pipelineContext);
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
		int numRectangles = 10_000;
		int minWidth = min(10, swapchainImage.width());
		int minHeight = min(10, swapchainImage.height());

		Vk2dColorBatch batch1 = colorPipeline.addBatch(frame, 6 * numRectangles);
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

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		colorPipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("FillColorBenchmark", 1, Vk2dValidationMode.NONE, FillColorBenchmark::new);
	}
}
