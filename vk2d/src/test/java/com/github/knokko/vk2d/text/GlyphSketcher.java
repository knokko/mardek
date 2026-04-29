package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.vk2d.Vk2dConfig;
import com.github.knokko.vk2d.Vk2dInstance;
import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_font_get_nominal_glyph;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;

public class GlyphSketcher {

	public static void main(String[] args) throws IOException {
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_0, "GlyphSketcher", 1
		).validation().forbidValidationErrors().build();

		var config = new Vk2dConfig();
		config.simpleText = true;
		var instance = new Vk2dInstance(boiler, config);

		var writer = new Vk2dResourceWriter();
		int fontDataThaana = writer.addFontBlob(TestFontCollection.thaanaFont());
		writer.addFont(fontDataThaana, 0);

		int fontDataMyriad = writer.addFontBlob(TestFontCollection.myriadFont());
		int fontMyriad = writer.addFont(fontDataMyriad, 0);

		var bundle = writer.directlyCreateBundle(instance, null);

		var font = bundle.getFont(fontMyriad);
		int glyph;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			var pGlyph = stack.callocInt(1);
			assertHbSuccess(hb_font_get_nominal_glyph(
					font.hbFont, '$', pGlyph
			), "font_get_nominal_glyph");
			glyph = pGlyph.get();
		}

		int l = font.outlines.getLinesOffset(glyph);
		int c = font.outlines.getCurvesOffset(glyph);

		var image = new BufferedImage(600, 800, BufferedImage.TYPE_INT_ARGB);
		var graphics = image.createGraphics();
		graphics.setColor(new Color(1f, 0f, 0f, 0.5f));
		//var outlineBuffer = font.outlines.buffer.intBuffer();
		IntBuffer outlineBuffer = null;

		int offsetX = 50;
		int offsetY = 50;
		for (int lineIndex = 0; lineIndex < font.outlines.getNumLines(glyph); lineIndex++) {
			outlineBuffer.position(l + 4 * lineIndex);
			graphics.drawLine(
					outlineBuffer.get() + offsetX, outlineBuffer.get() + offsetY,
					outlineBuffer.get() + offsetX, outlineBuffer.get() + offsetY
			);
		}

		for (int curveIndex = 0; curveIndex < font.outlines.getNumCurves(glyph); curveIndex++) {
			outlineBuffer.position(c + 6 * curveIndex);
			int startX = outlineBuffer.get() + offsetX;
			int startY = outlineBuffer.get() + offsetY;
			int controlX = outlineBuffer.get() + offsetX;
			int controlY = outlineBuffer.get() + offsetY;
			int endX = outlineBuffer.get() + offsetX;
			int endY = outlineBuffer.get() + offsetY;

			graphics.setColor(new Color(0f, 1f, 0, 0.5f));
			graphics.drawLine(startX, startY, controlX, controlY);
			graphics.drawLine(controlX, controlY, endX, endY);
			graphics.setColor(new Color(0f, 0f, 1f, 0.5f));
			graphics.drawLine(startX, startY, endX, endY);
		}

		graphics.dispose();
		ImageIO.write(image, "PNG", new File("glyph-sketch.png"));

		bundle.destroy(boiler);
		instance.destroy();
		boiler.destroyInitialObjects();
	}
}
