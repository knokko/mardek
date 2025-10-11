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

public class StructuredImageBenchmark extends Vk2dWindow {

	public StructuredImageBenchmark(VkbWindow window) {
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
		int numRounds = 1;
		int scale = 1;
		Vk2dImageBatch batch1 = pipelines.image.addBatch(frame.swapchainStage, 5000, resources);
		for (int round = 0; round < numRounds; round++) {
			for (int y = 0; y < swapchainImage.getHeight(); y += 16 * scale) {
				for (int x = 0; x < swapchainImage.getWidth(); x += 16 * scale) {
					batch1.simple(
							x, y, x + 16 * scale - 1, y + 16 * scale - 1,
							rng.nextInt(resources.numImages)
					);
				}
			}
		}
	}

	public static void main(String[] args) {
		bootstrap("StructuredImageBenchmark", 1, Vk2dValidationMode.NONE, StructuredImageBenchmark::new);
	}
}
