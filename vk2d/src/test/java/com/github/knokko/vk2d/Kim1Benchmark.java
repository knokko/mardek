package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dKim1Batch;
import com.github.knokko.vk2d.pipeline.Vk2dKim1Pipeline;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import static com.github.knokko.vk2d.ImageBenchmarkResourceWriter.FILE;

public class Kim1Benchmark extends Vk2dWindow {

	private Vk2dKim1Pipeline kim1Pipeline;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public Kim1Benchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(FILE.toPath());
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.kim1Pipeline = new Vk2dKim1Pipeline(pipelineContext, resources, shared);
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
		Vk2dKim1Batch batch1 = frame.addBatch(kim1Pipeline, 5_000);
		for (int round = 0; round < numRounds; round++) {
			for (int y = 0; y < swapchainImage.height(); y += 16 * scale) {
				for (int x = 0; x < swapchainImage.width(); x += 16 * scale) {
					batch1.simple(
							x, y, x + 16 * scale - 1, y + 16 * scale - 1,
							resources.getFakeImageOffset(rng.nextInt(resources.numFakeImages))
					);
				}
			}
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		kim1Pipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("Kim1Benchmark", 1, Vk2dValidationMode.NONE, Kim1Benchmark::new);
	}
}
