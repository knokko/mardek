package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.resource.Vk2dFont;
import com.github.knokko.vk2d.resource.Vk2dTextBuffer;

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
			int horizontalIntersections, int verticalIntersections,
			int fillColor, int strokeColor, int backgroundColor
	) {
		if (horizontalIntersections == -1 || verticalIntersections == -1) return;
		if (maxX < 0 || maxY < 0 || minX >= width || minY >= height) return;
		ByteBuffer vertices = putVertices(6);

		int width = 1 + maxX - minX;
		int height = 1 + maxY - minY;
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
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
				minX, minY, maxX, maxY,
				horizontalIntersections, verticalIntersections,
				fillColor, strokeColor, backgroundColor
		);
	}

	public void drawPrimitiveString(
			String text, float baseX, float baseY, Vk2dFont font, float heightA, int fillColor
	) {
		int strokeColor = rgba(red(fillColor), green(fillColor), blue(fillColor), (alpha(fillColor) & 0xFF) / 2);
		for (int charIndex = 0; charIndex < text.length(); charIndex++) {
			int glyph = font.getGlyphForChar(text.charAt(charIndex));
			float minX = baseX + heightA * font.getGlyphMinX(glyph);
			float minY = baseY - heightA * font.getGlyphMaxY(glyph);
			float maxX = baseX + heightA * font.getGlyphMaxX(glyph);
			float maxY = baseY + heightA * font.getGlyphMinY(glyph);
			System.out.println(font.getGlyphMinX(glyph) + " " + font.getGlyphMinY(glyph) + " " + font.getGlyphMaxX(glyph) + " " + font.getGlyphMaxY(glyph));

			int intMinX = (int) Math.floor(minX);
			int intMinY = (int) Math.floor(minY);
			int intMaxX = (int) Math.ceil(maxX);
			int intMaxY = (int) Math.ceil(maxY);

			float width =  1f + maxX - minX;
			float height = 1f + maxY - minY;
			int intWidth = 1 + intMaxX - intMinX;
			int intHeight = 1 + intMaxY - intMinY;

			float offsetX = minX - intMinX;
			float offsetY = minY - intMinY;
			int horizontalIndex = textBuffer.scratch(recorder, font, glyph, offsetY, height, intHeight, true);
			int verticalIndex = textBuffer.scratch(recorder, font, glyph, offsetX, width, intWidth, false);
			glyphBetween(
					intMinX, intMinY, intMaxX, intMaxY, horizontalIndex, verticalIndex,
					fillColor, strokeColor, 0
			);
			baseX += heightA * font.getGlyphAdvance(glyph);
		}
	}
}
