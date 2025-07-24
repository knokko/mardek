package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline;
import com.github.knokko.vk2d.resource.Vk2dFont;
import com.github.knokko.vk2d.resource.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.rgba;

public class GlyphBenchmark1 extends Vk2dWindow {

	private Vk2dGlyphPipeline textPipeline;
	private Vk2dTextBuffer textBuffer;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public GlyphBenchmark1(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() {
		return GlyphBenchmark1.class.getResourceAsStream("text-benchmark-resources.bin");
	}

	@Override
	protected void createResources(BoilerInstance boiler, MemoryCombiner combiner, DescriptorCombiner descriptors) {
		super.createResources(boiler, combiner, descriptors);
		this.sharedText = new Vk2dSharedText(boiler);
		this.textPipeline = new Vk2dGlyphPipeline(pipelineContext, sharedText);
		this.textBuffer = new Vk2dTextBuffer(boiler, combiner, sharedText, descriptors, numFramesInFlight);
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.textBuffer.initializeDescriptorSets(boiler);
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

		int heightA = 5;
		Vk2dFont font0 = resources.getFont(0);
		Vk2dFont font1 = resources.getFont(1);
		Vk2dGlyphBatch batch = textPipeline.addBatch(frame, 10_000, recorder, textBuffer);

		textBuffer.startFrame();
		int cellSize = 3 * heightA / 2;
		int round = 0;
		int glyph = 0;
		for (int y = cellSize; y < swapchainImage.height(); y += cellSize) {
			for (int x = 0; x < swapchainImage.width(); x += cellSize) {
				Vk2dFont font = glyph % 3 == 0 ? font0 : font1;
				if (glyph >= font.getNumGlyphs()) {
					glyph = 0;
					round += 1;
				}
				int glyphOffsetHorizontal = textBuffer.scratch(
						recorder, font, glyph, batch.determineHeight(font, heightA, glyph), true
				);
				int glyphOffsetVertical = textBuffer.scratch(
						recorder, font, glyph, batch.determineWidth(font, heightA, glyph), false
				);

				int fillColor;
				int strokeColor;
				int backgroundColor = 0;

				if (glyph % 3 == round % 3) {
					fillColor = rgb(255, 255, 255);
					strokeColor = rgb(255, 0, 0);
				} else if (glyph % 2 == round % 2) {
					fillColor = rgb(255, 255, 255);
					strokeColor = rgba(255, 255, 255, 128);
				} else {
					fillColor = 0;
					strokeColor = rgb(255, 0, 0);
				}
				batch.glyphAt(
						x, y, font, heightA, glyph, glyphOffsetHorizontal, glyphOffsetVertical,
						fillColor, strokeColor, backgroundColor
				);
				glyph += 1;
			}

			heightA += 1;
			cellSize = 3 * heightA / 2;
		}

		textBuffer.transfer(recorder, sharedText);
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		textPipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			bootstrap("GlyphBenchmark2", 1, Vk2dValidationMode.NONE, GlyphBenchmark2::new);
		} else {
			bootstrap("GlyphBenchmark1", 1, Vk2dValidationMode.NONE, GlyphBenchmark1::new);
		}
	}
}
