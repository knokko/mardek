package com.github.knokko.vk2d.showcase;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.window.AcquiredImage;
import com.github.knokko.boiler.window.VkbWindow;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dValidationMode;
import com.github.knokko.vk2d.Vk2dWindow;
import com.github.knokko.vk2d.frame.Vk2dSwapchainFrame;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import com.github.knokko.vk2d.text.TestFontCollection;
import com.github.knokko.vk2d.text.TextAlignment;
import com.github.knokko.vk2d.text.Vk2dTextStyle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.github.knokko.boiler.utilities.ColorPacker.rgb;
import static com.github.knokko.boiler.utilities.ColorPacker.srgbToLinear;

public class ShowcaseColorPipeline extends Vk2dWindow {

	public ShowcaseColorPipeline(VkbWindow window) {
		super(window, true);
	}

	@Override
	protected InputStream initialResourceBundle() throws IOException {
		var writer = new Vk2dResourceWriter();
		int fontDataThaana = writer.addFontBlob(TestFontCollection.thaanaFont());

		int fontThaana = writer.addFont(fontDataThaana, 0);
		writer.addFallbackAtlas(fontThaana, 8, 20f, 0.1f);

		var output = new ByteArrayOutputStream();
		writer.write(output, null);
		return new ByteArrayInputStream(output.toByteArray());
	}

	@Override
	protected void setupConfig(Vk2dConfig config) {
		config.simpleText = true;
		config.color = true;
	}

	@Override
	protected void renderFrame(Vk2dSwapchainFrame frame, int frameIndex, CommandRecorder recorder, AcquiredImage swapchainImage, BoilerInstance boiler) {
		var style = new Vk2dTextStyle.FillStyle(rgb(255, 255, 255)).only();
		var font = resources.getFont(0);
		var textBatch = pipelines.simpleText.addBatch(frame.swapchainStage, 1000, simpleTextStyleCache);
		textBatch.drawString(
				"colorBatch.fill(200, 100, 600, 500, srgbToLinear(rgb(200, 200, 0)))",
				400f, 80f, 15f, font, style, TextAlignment.CENTERED
		);
		textBatch.drawString(
				"colorBatch.gradient(900, 100, 1300, 600, rgb(200, 0, 0), rgb(0, 200, 0), rgb(0, 0, 200))",
				1250, 180f, 15f, font, style, TextAlignment.CENTERED
		);
		textBatch.drawString(
				"colorBatch.fillUnaligned(300, 600, 100, 800, 550, 1000, 700, 900, srgbToLinear(rgb(200, 0, 200)))",
				500f, 580f, 15f, font, style, TextAlignment.CENTERED
		);
		textBatch.drawString(
				"colorBatch.gradientUnaligned(900, 700, rgb(1f, 0f, 0f), 950, 950, rgb(1f, 0f, 0f), 1300, 900, rgb(1f, 1f, 0f), 1400, 700, rgb(1f, 1f, 0f))",
				1200, 680f, 15f, font, style, TextAlignment.CENTERED
		);

		var colorBatch = pipelines.color.addBatch(frame.swapchainStage, 10);
		colorBatch.fill(200, 100, 600, 500, srgbToLinear(rgb(200, 200, 0)));
		colorBatch.gradient(
				1000, 200, 1500, 600,
				rgb(200, 0, 0), rgb(0, 200, 0), rgb(0, 0, 200)
		);
		colorBatch.fillUnaligned(
				300, 600, 100, 800, 550, 1000, 700, 900,
				srgbToLinear(rgb(200, 0, 200))
		);
		colorBatch.gradientUnaligned(
				900, 700, rgb(1f, 0f, 0f),
				950, 950, rgb(1f, 0f, 0f),
				1300, 900, rgb(1f, 1f, 0f),
				1400, 700, rgb(1f, 1f, 0f)
		);
	}

	public static void main(String[] args) {
		Vk2dWindow.bootstrap("ShowcaseColorPipeline", 1, Vk2dValidationMode.WEAK, ShowcaseColorPipeline::new);
	}
}
