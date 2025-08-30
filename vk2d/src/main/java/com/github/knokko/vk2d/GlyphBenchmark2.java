package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;
import com.github.knokko.vk2d.text.Vk2dFont;
import org.lwjgl.sdl.*;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.github.knokko.boiler.exceptions.SDLFailureException.assertSdlSuccess;
import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static java.lang.Math.max;
import static org.lwjgl.sdl.SDLKeycode.*;

public class GlyphBenchmark2 extends Vk2dWindow {

	private static final List<String> SHADER_CODE = new ArrayList<>();

	static {
		InputStream input = GlyphBenchmark2.class.getResourceAsStream("glyph/scratch.comp");
		assert input != null;
		Scanner scanner = new Scanner(input);
		while (scanner.hasNextLine()) {
			SHADER_CODE.add(scanner.nextLine());
		}
		scanner.close();
	}

	private boolean shiftDown, controlDown;
	private float offsetX, offsetY;
	private int heightA = 12;
	private int rawStrokeWidth = 0;
	private int fontIndex = 0;

	public GlyphBenchmark2(VkbWindow window) {
		super(window, false);
	}

	@Override
	protected InputStream initialResourceBundle() {
		return GlyphBenchmark2.class.getResourceAsStream("text-benchmark-resources.bin");
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.text = true;
	}

	@Override
	@SuppressWarnings("resource")
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);

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
				if (event.key() == SDLK_LSHIFT || event.key() == SDLK_RSHIFT) {
					shiftDown = event.down();
				}
				if (event.key() == SDLK_LCTRL || event.key() == SDLK_RCTRL) {
					controlDown = event.down();
				}
				if (event.key() == SDLK_F && event.down()) {
					if (shiftDown) {
						fontIndex -= 1;
						if (fontIndex < 0) fontIndex += resources.getNumFonts();
					} else fontIndex = (fontIndex + 1) % resources.getNumFonts();
				}
				if (event.key() == SDLK_S && event.down()) {
					if (shiftDown) rawStrokeWidth -= 1;
					else rawStrokeWidth += 1;
					System.out.println("new stroke width is " + 0.1f * rawStrokeWidth);
				}
			}
			return false;
		}, 0L), "AddEventWatch");
	}

	@Override
	protected void renderFrame(
			Vk2dSwapchainFrame frame, int frameIndex,
			CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler
	) {
		printFps();

		int lineHeight = 11 * heightA / 6;
		Vk2dFont font = resources.getFont(fontIndex);
		Vk2dGlyphBatch batch = pipelines.text.addBatch(
				frame.swapchainStage, 15_000, recorder, textBuffer, perFrameDescriptorSet
		);

		int baseY = lineHeight + (int) offsetY;
		for (String line : SHADER_CODE) {
			float baseX = 50 + offsetX;
			batch.drawPrimitiveString(
					line, baseX, baseY, font, heightA, rgb(255, 255, 255),
					rgb(0, 0, 255), rawStrokeWidth * 0.1f
			);
			baseY += lineHeight;
		}
	}

	public static void main(String[] args) {
		bootstrap("GlyphBenchmark2", 1, Vk2dValidationMode.STRONG, GlyphBenchmark2::new);
	}
}
