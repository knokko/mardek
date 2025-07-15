package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dTextBatch;
import com.github.knokko.vk2d.pipeline.Vk2dTextPipeline;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;

import static com.github.knokko.vk2d.TextBenchmarkResourceWriter.TEXT_RESOURCE_FILE;

public class TextBenchmark extends Vk2dWindow {

	private Vk2dTextPipeline textPipeline;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public TextBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(TEXT_RESOURCE_FILE.toPath());
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.textPipeline = new Vk2dTextPipeline(pipelineContext, shared);
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

		Vk2dTextBatch batch1 = textPipeline.addBatch(frame, 6, resources.getFont(0));
		int glyph = 0;
		int cellSize = 50;
		Random rng = new Random();
		for (int y = 0; y < swapchainImage.height(); y += cellSize) {
			for (int x = 0; x < swapchainImage.width(); x += cellSize) {
				if (glyph >= batch1.font.getNumGlyphs()) glyph = 0;
				batch1.glyphAt(x, y, 0.8f * cellSize, glyph, rng.nextInt());
				glyph += 1;
			}
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		textPipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("TextBenchmark", 1, Vk2dValidationMode.NONE, TextBenchmark::new);
	}
}
