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
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess;
import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static java.lang.Math.max;

public class GlyphBenchmark2 extends Vk2dWindow {

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
	protected InputStream initialResourceBundle() {
		return GlyphBenchmark2.class.getResourceAsStream("text-benchmark-resources.bin");
	}

	@Override
	protected void createResources(BoilerInstance boiler, MemoryCombiner combiner, DescriptorCombiner descriptors) {
		super.createResources(boiler, combiner, descriptors);
		this.sharedText = new Vk2dSharedText(boiler);
		this.textPipeline = new Vk2dGlyphPipeline(pipelineContext, sharedText);
		this.textBuffer = new Vk2dTextBuffer(boiler, combiner, sharedText, descriptors, numFramesInFlight);
	}

	private boolean shiftDown, controlDown;
	private float offsetX, offsetY;
	private int heightA = 12;
	private int fontIndex = 0;

	@Override
	@SuppressWarnings("resource")
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);
		this.textBuffer.initializeDescriptorSets(boiler);

		assertSdlSuccess(SDLEvents.SDL_AddEventWatch((userData, rawEvent) -> {
			int type = SDL_Event.ntype(rawEvent);
			if (type == SDLEvents.SDL_EVENT_MOUSE_WHEEL) {
				SDL_MouseWheelEvent event = SDL_MouseWheelEvent.create(rawEvent);
				float x = event.x();
				float y = event.y();

				if (shiftDown) {
					//noinspection SuspiciousNameCombination
					x = y;
					y = 0f;
				}

				if (controlDown) {
					if (y > 0) heightA++;
					else heightA = max(1, heightA - 1);
				} else {
					float speed = 2f * heightA;
					offsetX += speed * x;
					offsetY += speed * y;
				}
			}

			if (type == SDLEvents.SDL_EVENT_KEY_DOWN || type == SDLEvents.SDL_EVENT_KEY_UP) {
				SDL_KeyboardEvent event = SDL_KeyboardEvent.create(rawEvent);
				if (event.key() == SDLKeycode.SDLK_LSHIFT || event.key() == SDLKeycode.SDLK_RSHIFT) {
					shiftDown = event.down();
				}
				if (event.key() == SDLKeycode.SDLK_LCTRL || event.key() == SDLKeycode.SDLK_RCTRL) {
					controlDown = event.down();
				}
				if (event.key() == SDLKeycode.SDLK_F && event.down()) {
					fontIndex = (fontIndex + 1) % resources.getNumFonts();
				}
			}
			return false;
		}, 0L), "AddEventWatch");
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

		int lineHeight = 11 * heightA / 6;
		Vk2dFont font = resources.getFont(fontIndex);
		Vk2dGlyphBatch batch = textPipeline.addBatch(frame, 60_000, textBuffer.getRenderDescriptorSet());

		textBuffer.startFrame();

		int baseY = lineHeight + (int) offsetY;
		int whitespaceGlyph = font.getGlyphForChar(' ');
		float whitespaceAdvance = font.getGlyphAdvance(whitespaceGlyph);
		for (String line : SHADER_CODE) {
			int baseX = 50 + (int) offsetX;
			for (int x = 0; x < line.length(); x++) {
				int charCode = line.charAt(x);
				if (charCode == '\t') {
					baseX += (int) (4f * whitespaceAdvance * heightA);
					continue;
				}
				int glyph = font.getGlyphForChar(line.charAt(x));
				int glyphOffsetHorizontal = textBuffer.scratch(
						recorder, sharedText, font, glyph, batch.determineHeight(font, heightA, glyph), true
				);
				int glyphOffsetVertical = textBuffer.scratch(
						recorder, sharedText, font, glyph, batch.determineWidth(font, heightA, glyph), false
				);
				batch.glyphAt(
						baseX + (int) (heightA * font.getGlyphMinX(glyph)), baseY, font, heightA, glyph, glyphOffsetHorizontal, glyphOffsetVertical,
						rgb(255, 255, 255), 0, 0
				);
				baseX += (int) (heightA * font.getGlyphAdvance(glyph));
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
