package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.text.Vk2dFont;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;

import java.nio.ByteBuffer;

import static com.github.knokko.boiler.utilities.ColorPacker.*;

public class Vk2dGlyphBatch extends Vk2dBatch {

	private final CommandRecorder recorder;
	public final Vk2dTextBuffer textBuffer;

	public Vk2dGlyphBatch(
			Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity,
			CommandRecorder recorder, Vk2dTextBuffer textBuffer
	) {
		super(pipeline, frame, initialCapacity);
		this.recorder = recorder;
		this.textBuffer = textBuffer;
	}

	public void glyphBetween(
			int minX, int minY, int maxX, int maxY,
			float subpixelX, float subpixelY, float width, float height,
			int horizontalIntersections, int verticalIntersections,
			int fillColor, int strokeColor, int backgroundColor
	) {
		if (horizontalIntersections == -1 || verticalIntersections == -1) return;
		if (maxX < 0 || maxY < 0 || minX >= this.width || minY >= this.height) return;
		ByteBuffer vertices = putVertices(6);

		int intWidth = 1 + maxX - minX;
		int intHeight = 1 + maxY - minY;
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(subpixelX).putFloat(subpixelY);
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections);
		vertices.putFloat(width).putFloat(height);
		vertices.putInt(intWidth).putInt(intHeight);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(subpixelX).putFloat(subpixelY);
		vertices.putFloat(1f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections);
		vertices.putFloat(width).putFloat(height);
		vertices.putInt(intWidth).putInt(intHeight);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(subpixelX).putFloat(subpixelY);
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections);
		vertices.putFloat(width).putFloat(height);
		vertices.putInt(intWidth).putInt(intHeight);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);


		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(subpixelX).putFloat(subpixelY);
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections);
		vertices.putFloat(width).putFloat(height);
		vertices.putInt(intWidth).putInt(intHeight);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(subpixelX).putFloat(subpixelY);
		vertices.putFloat(0f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections);
		vertices.putFloat(width).putFloat(height);
		vertices.putInt(intWidth).putInt(intHeight);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(subpixelX).putFloat(subpixelY);
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections);
		vertices.putFloat(width).putFloat(height);
		vertices.putInt(intWidth).putInt(intHeight);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);
	}

	public int determineWidth(Vk2dFont font, float heightA, int glyph) {
		return (int) (heightA * (font.getGlyphMaxX(glyph) - font.getGlyphMinX(glyph)));
	}

	public int determineHeight(Vk2dFont font, float heightA, int glyph) {
		return (int) (heightA * (font.getGlyphMaxY(glyph) - font.getGlyphMinY(glyph)));
	}

	public void glyphAt(
			float baseX, float baseY, Vk2dFont font, float heightA, int glyph,
			int horizontalIntersections, int verticalIntersections,
			int fillColor, int strokeColor, int backgroundColor
	) {
		if (font.getNumCurves(glyph) == 0) return;
		float perfectMinX = baseX + heightA * font.getGlyphMinX(glyph);
		float perfectMinY = baseY - heightA * font.getGlyphMaxY(glyph);
		int minX = (int) perfectMinX;
		int minY = (int) perfectMinY;
		float perfectMaxY = baseY + heightA * font.getGlyphMinY(glyph);
		int maxX = minX + determineWidth(font, heightA, glyph) - 1;
		int maxY = minY + determineHeight(font, heightA, glyph) - 1;
		glyphBetween(
				minX, minY, maxX, maxY, 0f, 0f, 1f + maxX - minX, 1f + maxY - minY,
				horizontalIntersections, verticalIntersections,
				fillColor, strokeColor, backgroundColor
		);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA, Vk2dFont font,
			int fillColor, int strokeColor
	) {
		// TODO Add HB support
		drawPrimitiveString(text, baseX, baseY, font, heightA, fillColor, strokeColor);
	}

	public void drawPrimitiveString(
			String text, float baseX, float baseY, Vk2dFont font, float heightA, int fillColor
	) {
		// TODO Add changeAlpha method to vk-boiler
		int strokeColor = rgba(red(fillColor), green(fillColor), blue(fillColor), (alpha(fillColor) & 0xFF) / 2);
		drawPrimitiveString(text, baseX, baseY, font, heightA, fillColor, strokeColor);
	}

	public void drawPrimitiveString(
			String text, float baseX, float baseY, Vk2dFont font, float heightA, int fillColor, int strokeColor
	) {

		for (int charIndex = 0; charIndex < text.length(); charIndex++) {
			char nextChar = text.charAt(charIndex);
			if (nextChar == '\t') {
				baseX += 4f * heightA * font.getWhitespaceAdvance();
				continue;
			}

			int glyph = font.getGlyphForChar(nextChar);
			float minX = baseX + heightA * font.getGlyphMinX(glyph);
			float minY = baseY - heightA * font.getGlyphMaxY(glyph);
			float maxX = baseX + heightA * font.getGlyphMaxX(glyph);
			float maxY = baseY - heightA * font.getGlyphMinY(glyph);

			int intMinX = (int) Math.floor(minX);
			int intMinY = (int) Math.floor(minY);
			int intBoundX = (int) Math.ceil(maxX);
			int intBoundY = (int) Math.ceil(maxY);

			float width = maxX - minX;
			float height = maxY - minY;
			int intWidth = intBoundX - intMinX;
			int intHeight = intBoundY - intMinY;

			float offsetX = minX - intMinX;
			float offsetY = intBoundY - maxY;
			int horizontalIndex = textBuffer.scratch(recorder, font, glyph, -offsetY, height, intHeight, true);
			int verticalIndex = textBuffer.scratch(recorder, font, glyph, -offsetX, width, intWidth, false);
			glyphBetween(
					intMinX, intMinY, intBoundX - 1, intBoundY - 1,
					-offsetX, -offsetY, width, height,
					horizontalIndex, verticalIndex, fillColor, strokeColor, 0
			);
			baseX += heightA * font.getGlyphAdvance(glyph);
		}
	}
}
