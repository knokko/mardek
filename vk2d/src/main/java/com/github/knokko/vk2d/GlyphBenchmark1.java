package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;
import com.github.knokko.vk2d.text.Vk2dFont;

import java.io.InputStream;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;

public class GlyphBenchmark1 extends Vk2dWindow {

	public GlyphBenchmark1(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() {
		return GlyphBenchmark1.class.getResourceAsStream("text-benchmark-resources.bin");
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.text = true;
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();

		int heightA = 5;
		Vk2dFont font0 = resources.getFont(0);
		Vk2dFont font1 = resources.getFont(1);
		Vk2dGlyphBatch batch = pipelines.text.addBatch(
				frame.swapchainStage, 10_000, recorder, textBuffer, perFrameDescriptorSet
		);

		int cellSize = 3 * heightA / 2;
		int round = 0;
		int glyph = 0;
		for (int y = cellSize; y < swapchainImage.getHeight(); y += cellSize) {
			for (int x = 0; x < swapchainImage.getWidth(); x += cellSize) {
				Vk2dFont font = glyph % 3 == 0 ? font0 : font1;
				if (glyph >= font.getNumGlyphs()) {
					glyph = 0;
					round += 1;
				}

				int fillColor;
				int strokeColor;
				float strokeWidth;

				if (glyph % 3 == round % 3) {
					fillColor = rgb(255, 255, 255);
					strokeColor = rgb(255, 0, 0);
					strokeWidth = 1f;
				} else if (glyph % 2 == round % 2) {
					fillColor = rgb(255, 255, 255);
					strokeColor = 0;
					strokeWidth = 0f;
				} else {
					fillColor = 0;
					strokeColor = rgb(255, 0, 0);
					strokeWidth = 1f;
				}
				batch.glyphAt(
						x, y, font, heightA, glyph,
						fillColor, strokeColor, strokeWidth
				);
				glyph += 1;
			}

			heightA += 1;
			cellSize = 3 * heightA / 2;
		}
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			bootstrap("GlyphBenchmark2", 1, Vk2dValidationMode.NONE, GlyphBenchmark2::new);
		} else {
			bootstrap("GlyphBenchmark1", 1, Vk2dValidationMode.STRONG, GlyphBenchmark1::new);
		}
	}
}
