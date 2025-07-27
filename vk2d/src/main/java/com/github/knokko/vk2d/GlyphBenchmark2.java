package com.github.knokko.vk2d;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.batch.Vk2dGlyphBatch;
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

	private long referenceTime = System.nanoTime();
	private int fps = 0;

	private boolean shiftDown, controlDown;
	private float offsetX, offsetY;
	private int heightA = 12;
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
				if (event.key() == SDLKeycode.SDLK_LSHIFT || event.key() == SDLKeycode.SDLK_RSHIFT) {
					shiftDown = event.down();
				}
				if (event.key() == SDLKeycode.SDLK_LCTRL || event.key() == SDLKeycode.SDLK_RCTRL) {
					controlDown = event.down();
				}
				if (event.key() == SDLKeycode.SDLK_F && event.down()) {
					if (shiftDown) {
						fontIndex -= 1;
						if (fontIndex < 0) fontIndex += resources.getNumFonts();
					} else fontIndex = (fontIndex + 1) % resources.getNumFonts();
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
		Vk2dGlyphBatch batch = pipelines.text.addBatch(frame, 50_000, recorder, textBuffer);

		int baseY = lineHeight + (int) offsetY;
		for (String line : SHADER_CODE) {
			float baseX = 50 + offsetX;
			batch.drawPrimitiveString(line, baseX, baseY, font, heightA, rgb(255, 255, 255));
			baseY += lineHeight;
		}
	}

	public static void main(String[] args) {
		bootstrap("GlyphBenchmark2", 1, Vk2dValidationMode.NONE, GlyphBenchmark2::new);
	}
}
