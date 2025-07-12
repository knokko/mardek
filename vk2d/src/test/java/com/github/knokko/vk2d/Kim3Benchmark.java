package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dKimBatch;
import com.github.knokko.vk2d.pipeline.Vk2dKimPipeline;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import static com.github.knokko.vk2d.ImageBenchmarkResourceWriter.FILE;

public class Kim3Benchmark extends Vk2dWindow {

	private Vk2dKimPipeline kim3Pipeline;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public Kim3Benchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(FILE.toPath());
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.kim3Pipeline = new Vk2dKimPipeline(pipelineContext, shared, 3);
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
		Vk2dKimBatch batch1 = kim3Pipeline.addBatch(frame, 5000, resources);
		for (int round = 0; round < numRounds; round++) {
			for (int y = 0; y < swapchainImage.height(); y += 16 * scale) {
				for (int x = 0; x < swapchainImage.width(); x += 16 * scale) {
					batch1.simple(
							x, y, x + 16 * scale - 1, y + 16 * scale - 1,
							1 + 2 * rng.nextInt(resources.numFakeImages / 2)
					);
				}
			}
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		kim3Pipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("Kim3Benchmark", 1, Vk2dValidationMode.NONE, Kim3Benchmark::new);
	}
}
