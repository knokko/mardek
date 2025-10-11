package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dKimBatch;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import static com.github.knokko.vk2d.ImageBenchmarkResourceWriter.FILE;

public class Kim1Benchmark extends Vk2dWindow {

	public Kim1Benchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(FILE.toPath());
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.kim1 = true;
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();

		Random rng = new Random();
		int numRounds = 1;
		int scale = 1;
		Vk2dKimBatch batch1 = pipelines.kim1.addBatch(frame.swapchainStage, 5000, resources);
		for (int round = 0; round < numRounds; round++) {
			for (int y = 0; y < swapchainImage.getHeight(); y += 16 * scale) {
				for (int x = 0; x < swapchainImage.getWidth(); x += 16 * scale) {
					batch1.simple(
							x, y, x + 16 * scale - 1, y + 16 * scale - 1,
							2 * rng.nextInt(resources.numFakeImages / 2)
					);
				}
			}
		}
	}

	public static void main(String[] args) {
		bootstrap("Kim1Benchmark", 1, Vk2dValidationMode.NONE, Kim1Benchmark::new);
	}
}
