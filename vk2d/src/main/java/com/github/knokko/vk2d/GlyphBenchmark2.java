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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.rgba;
import static java.lang.Math.max;

public class GlyphBenchmark2 extends Vk2dWindow {

	private static final File TEXT_RESOURCE_FILE = new File("text-benchmark-resources.bin");
	private static final List<String> SHADER_CODE = new ArrayList<>();

	static {
		InputStream input = GlyphBenchmark2.class.getResourceAsStream("glyph.frag");
		assert input != null;
		Scanner scanner = new Scanner(input);
		while (scanner.hasNextLine()) {
			SHADER_CODE.add(scanner.nextLine());
		}
		scanner.close();
	}

	private Vk2dGlyphPipeline textPipeline;
	private Vk2dTextBuffer textBuffer;

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	public GlyphBenchmark2(VkbWindow window) {
		super(window, true);
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

		int heightA = 30;
		int lineHeight = 3 * heightA / 2;
		Vk2dFont font = resources.getFont(0);
		Vk2dGlyphBatch batch = textPipeline.addBatch(frame, 60_000, textBuffer.getRenderDescriptorSet());

		textBuffer.startFrame();

		int baseY = lineHeight;
		for (String line : SHADER_CODE) {
			int baseX = 50;
			for (int x = 0; x < line.length(); x++) {
				int glyph = font.getGlyphForChar(line.charAt(x));
				baseX += (int) (heightA * font.getGlyphMinX(glyph));
				int glyphOffsetHorizontal = textBuffer.scratch(
						recorder, sharedText, font, glyph, batch.determineHeight(font, heightA, glyph), true
				);
				int glyphOffsetVertical = textBuffer.scratch(
						recorder, sharedText, font, glyph, batch.determineWidth(font, heightA, glyph), false
				);
				batch.glyphAt(
						baseX, baseY, font, heightA, glyph, glyphOffsetHorizontal, glyphOffsetVertical,
						rgb(255, 255, 255), rgba(255, 255, 255, 128), 0
				);
				baseX += (int) (heightA * max(0.8f, font.getGlyphMaxX(glyph) - font.getGlyphMinX(glyph)));
			}
			baseY += lineHeight;
		}

		textBuffer.transfer(recorder, sharedText);
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		textPipeline.destroy(boiler);
	}

	public static void main(String[] args) {
		bootstrap("GlyphBenchmark2", 1, Vk2dValidationMode.STRONG, GlyphBenchmark2::new);
	}
}
