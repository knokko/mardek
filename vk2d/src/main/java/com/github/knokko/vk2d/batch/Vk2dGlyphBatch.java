package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.text.Vk2dFont;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;

import java.nio.ByteBuffer;

import static com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline.GLYPH_SIZE;
import static java.lang.Math.max;

public class Vk2dGlyphBatch extends Vk2dBatch {

	private final CommandRecorder recorder;
	public final Vk2dTextBuffer textBuffer;
	public final long perFrameDescriptorSet;

	public Vk2dGlyphBatch(
			Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity,
			CommandRecorder recorder, Vk2dTextBuffer textBuffer, long perFrameDescriptorSet
	) {
		super(pipeline, frame, initialCapacity);
		this.recorder = recorder;
		this.textBuffer = textBuffer;
		this.perFrameDescriptorSet = perFrameDescriptorSet;
	}

	public void glyphBetween(
			int minX, int minY, int maxX, int maxY,
			float subpixelX, float subpixelY, float width, float height,
			int horizontalIntersections, int verticalIntersections,
			int fillColor, int strokeColor, int backgroundColor, float strokeWidth
	) {
		if (horizontalIntersections == -1 || verticalIntersections == -1) return;
		if (maxX < 0 || maxY < 0 || minX >= this.width || minY >= this.height) return;

		BatchVertexData triangles = putTriangles(2);

		ByteBuffer glyphInfo = triangles.vertexData()[1];
		int glyphByteOffset = Math.toIntExact(glyphInfo.position() + triangles.vertexBuffers()[1].offset - perFrameBuffer.buffer.offset);
		int glyphIndex = glyphByteOffset / GLYPH_SIZE;

		putCompressedPosition(glyphInfo, minX, minY);
		glyphInfo.putFloat(subpixelX).putFloat(subpixelY);
		glyphInfo.putInt(horizontalIntersections).putInt(verticalIntersections);
		glyphInfo.putFloat(width).putFloat(height);
		glyphInfo.putInt(1 + maxX - minX).putInt(1 + maxY - minY);
		glyphInfo.putInt(fillColor).putInt(strokeColor).putInt(backgroundColor);
		glyphInfo.putFloat(strokeWidth);

		ByteBuffer vertices = triangles.vertexData()[0];
		vertices.putInt(glyphIndex);
		vertices.putInt(glyphIndex);
		vertices.putInt(glyphIndex);

		vertices.putInt(glyphIndex);
		vertices.putInt(glyphIndex);
		vertices.putInt(glyphIndex);
	}

	public void glyphAt(
			float baseX, float baseY, Vk2dFont font, float heightA, int glyph,
			int fillColor, int strokeColor, int backgroundColor, float strokeWidth
	) {
		if (font.getNumCurves(glyph) == 0) return;
		float minX = baseX + heightA * font.getGlyphMinX(glyph);
		float minY = baseY - heightA * font.getGlyphMaxY(glyph);
		float maxX = baseX + heightA * font.getGlyphMaxX(glyph);
		float maxY = baseY - heightA * font.getGlyphMinY(glyph);

		float strokeMargin = determineStrokeMargin(strokeWidth);
		int intMinX = (int) Math.floor(minX - strokeMargin);
		int intMinY = (int) Math.floor(minY - strokeMargin);
		int intBoundX = (int) Math.ceil(maxX + strokeMargin);
		int intBoundY = (int) Math.ceil(maxY + strokeMargin);

		float width = maxX - minX;
		float height = maxY - minY;
		int intWidth = intBoundX - intMinX;
		int intHeight = intBoundY - intMinY;
		float offsetX = minX - intMinX;
		float offsetY = intBoundY - maxY;

		int glyphOffsetHorizontal = textBuffer.scratch(
				recorder, font, glyph, -offsetY, height, intHeight, true
		);
		int glyphOffsetVertical = textBuffer.scratch(
				recorder, font, glyph, -offsetX, width, intWidth, false
		);

		glyphBetween(
				intMinX, intMinY, intBoundX - 1, intBoundY - 1, -offsetX, -offsetY, width, height,
				glyphOffsetHorizontal, glyphOffsetVertical,
				fillColor, strokeColor, backgroundColor, strokeWidth
		);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA, Vk2dFont font, int fillColor
	) {
		drawString(text, baseX, baseY, heightA, font, fillColor, 0, 0f);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA,
			Vk2dFont font, int fillColor, int strokeColor, float strokeWidth
	) {
		// TODO Add HB support
		drawPrimitiveString(text, baseX, baseY, font, heightA, fillColor, strokeColor, strokeWidth);
	}

	public void drawShadowedString(
			String text, float baseX, float baseY, float heightA,
			Vk2dFont font, int fillColor, int strokeColor, float strokeWidth,
			int shadowColor, float shadowOffsetX, float shadowOffsetY
	) {
		drawString(text, baseX + shadowOffsetX, baseY + shadowOffsetY, heightA, font, shadowColor);
		drawString(text, baseX, baseY, heightA, font, fillColor, strokeColor, strokeWidth);
	}

	public void drawPrimitiveString(
			String text, float baseX, float baseY, Vk2dFont font, float heightA, int fillColor
	) {
		drawPrimitiveString(text, baseX, baseY, font, heightA, fillColor, 0, 0f);
	}

	private float determineStrokeMargin(float strokeWidth) {
		// See determineStrokeIntensity in glyph.frag
		if (strokeWidth < 0.01f) return 0f;
		return max(1f, 0.5f + 0.5f * strokeWidth);
	}

	public void drawPrimitiveString(
			String text, float baseX, float baseY, Vk2dFont font, float heightA,
			int fillColor, int strokeColor, float strokeWidth
	) {

		for (int charIndex = 0; charIndex < text.length(); charIndex++) {
			char nextChar = text.charAt(charIndex);
			if (nextChar == '\t') {
				baseX += 4f * heightA * font.getWhitespaceAdvance();
				continue;
			}

			int glyph = font.getGlyphForChar(nextChar);
			glyphAt(baseX, baseY, font, heightA, glyph, fillColor, strokeColor, 0, strokeWidth);

			baseX += heightA * font.getGlyphAdvance(glyph);
		}
	}
}
