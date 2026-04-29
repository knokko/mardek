package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dValidationMode;
import com.github.knokko.vk2d.Vk2dWindow;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLKeycode.SDLK_E;
import static org.lwjgl.sdl.SDLKeycode.SDLK_Q;

public class TextPlayground extends Vk2dWindow {

	public TextPlayground(VkbWindow window) {
		super(window, true);
	}

	private boolean useFancy;
	private float rotation;

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		var writer = new Vk2dResourceWriter();
		int fontDataThaana = writer.addFontBlob(TestFontCollection.thaanaFont());

		int fontThaana = writer.addFont(fontDataThaana, 0);
		writer.addFallbackAtlas(fontThaana, 8, 200f, 0.1f);

		var output = new ByteArrayOutputStream();
		writer.write(output, null);
		return new ByteArrayInputStream(output.toByteArray());
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.simpleText = true;
		config.fancyText = true;
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		super.setup(boiler, stack);

		SDL_AddEventWatch((userData, rawEvent) -> {
			@SuppressWarnings("resource")
			var event = SDL_Event.create(rawEvent);
			if (event.type() == SDL_EVENT_MOUSE_BUTTON_DOWN) {
				useFancy = !useFancy;
			}

			if (event.type() == SDL_EVENT_KEY_DOWN) {
				@SuppressWarnings("resource")
				var keyEvent = SDL_KeyboardEvent.create(rawEvent);
				if (keyEvent.key() == SDLK_Q) rotation += 5f;
				if (keyEvent.key() == SDLK_E) rotation -= 5f;
			}

			return false;
		}, 0L);
	}

	@Override
	protected void renderFrame(Vk2dSwapchainFrame frame, int frameIndex, CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler) {
		printFps();

		String text = "MARDEK";
		var font = resources.getFont(0);

		float baseX = swapchainImage.getWidth() * 0.5f;
		float baseY = swapchainImage.getHeight() * 0.95f;
		float heightA = swapchainImage.getHeight() * 0.3f;

		if (useFancy) {
			int outerColor = srgbToLinear(rgb(107, 53, 4));
			int quarterColor = srgbToLinear(rgb(185, 93, 68));
			int middleColor = srgbToLinear(rgb(230, 187, 178));
			int innerBorderColor = srgbToLinear(rgb(68, 51, 34));
			int outerBorderColor = srgbToLinear(rgb(190, 144, 95));
			var style = new Vk2dFancyTextStyle(
					new Vk2dFancyTextStyle.Gradient(
						outerColor, quarterColor, middleColor, quarterColor, outerColor,
						0.3f, 0.4f, 0.5f, 1f
					), 1f, 0f,
					Vk2dFancyTextStyle.Gradient.plain(0),
					new Vk2dFancyTextStyle.Gradient(
						innerBorderColor, innerBorderColor, outerBorderColor, outerBorderColor, 0,
						0.04f, 0.045f, 0.07f, 0.075f
					), true
			);
			var batch = pipelines.fancyText.addBatch(frame.swapchainStage, 1000, fancyTextStyleCache);
			batch.drawString(text, baseX, baseY, rotation, heightA, font, style, TextAlignment.CENTERED);
		} else {
			int fillColor = srgbToLinear(rgb(110, 55, 5));
			int strokeColor = 0;
			var style = new Vk2dTextStyle(
					new Vk2dTextStyle.FillStyle(fillColor),
					new Vk2dTextStyle.StrokeStyle(strokeColor, 0.05f, true, 1f)
			);
			var batch = pipelines.simpleText.addBatch(frame.swapchainStage, 1000, simpleTextStyleCache);
			batch.drawString(text, baseX, baseY, heightA, font, style, TextAlignment.LEFT);
		}
	}

	public static void main(String[] args) {
		bootstrap("TextPlayground", 1, Vk2dValidationMode.STRONG, TextPlayground::new);
	}
}
