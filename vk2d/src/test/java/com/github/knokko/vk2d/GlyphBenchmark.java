package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline;
import com.github.knokko.vk2d.resource.Vk2dTextBuffer;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static com.github.knokko.vk2d.TextBenchmarkResourceWriter.TEXT_RESOURCE_FILE;

public class GlyphBenchmark extends Vk2dWindow {

	private Vk2dSharedText sharedText;
	private Vk2dGlyphPipeline textPipeline;
	private Vk2dTextBuffer textBuffer;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public GlyphBenchmark(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		return Files.newInputStream(TEXT_RESOURCE_FILE.toPath());
	}

	@Override
	protected void createResources(BoilerInstance boiler, MemoryCombiner combiner, DescriptorCombiner descriptors) {
		super.createResources(boiler, combiner, descriptors);
		this.sharedText = new Vk2dSharedText(boiler);
		this.textPipeline = new Vk2dGlyphPipeline(pipelineContext, sharedText);
		this.textBuffer = new Vk2dTextBuffer(boiler, combiner);
		this.textBuffer.requestDescriptorSets(sharedText, descriptors);
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.textBuffer.initializeDescriptorSets(boiler, resources.getFont(0));
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

		int glyphA = 4;
		int glyphHeight = 100;
		textBuffer.prepareScratch(recorder, sharedText);
		int offsetA = textBuffer.scratch(recorder, sharedText, glyphA, glyphHeight);
		textBuffer.prepareTransfer(recorder, sharedText);
		textBuffer.transfer(recorder, sharedText);
		Vk2dGlyphBatch batch = textPipeline.addBatch(frame, 6, textBuffer.getRenderDescriptorSet());
		int cellSize = 100;
		for (int y = 0; y < swapchainImage.height(); y += cellSize) {
			for (int x = 0; x < swapchainImage.width(); x += cellSize) {
				batch.simple(x, y, x + cellSize, y + glyphHeight - 1, offsetA);
			}
		}
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		textPipeline.destroy(boiler);
		sharedText.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("GlyphBenchmark", 1, Vk2dValidationMode.NONE, GlyphBenchmark::new);
	}
}
