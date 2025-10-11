package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dOvalBatch;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;

import java.util.Random;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;

public class OvalBenchmark extends Vk2dWindow {

	public OvalBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.oval = true;
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();

		Vk2dOvalBatch batch1 = pipelines.oval.addBatch(frame.swapchainStage, perFrameDescriptorSet, 24);
		batch1.simpleAliased(0, 0, batch1.width - 1, batch1.height - 1, rgb(255, 0, 0));
		batch1.simpleAntiAliased(
				batch1.width / 10, batch1.height / 10,
				batch1.width - batch1.width / 10, batch1.height - batch1.height / 10,
				0.1f, rgb(0, 255, 255)
		);
		batch1.aliased(
				200, 100, 300, 300, 300, 200,
				100, 100, rgb(255, 255, 0)
		);
		batch1.antiAliased(
				batch1.width - 320, batch1.height - 320, batch1.width - 180, batch1.height - 80,
				batch1.width - 300, batch1.height - 200,
				100, 100, 0.1f, rgb(255, 0, 255)
		);

		int cellSize = 25;
		Random rng = new Random();
		for (int y = 0; y < swapchainImage.getHeight(); y += cellSize) {
			for (int x = 0; x < swapchainImage.getWidth(); x += cellSize) {
				batch1.complex(
						x, y, x + cellSize - 1, y + cellSize - 1,
						x + cellSize / 2f, y + cellSize / 2f, cellSize / 2f, cellSize / 2f,
						rng.nextInt(), rng.nextInt(), rng.nextInt(), rng.nextInt(), rng.nextInt(),
						0.2f, 0.4f, 0.6f, 0.8f
				);
			}
		}
	}

	public static void main(String[] args) {
		bootstrap("OvalBenchmark", 1, Vk2dValidationMode.NONE, OvalBenchmark::new);
	}
}
