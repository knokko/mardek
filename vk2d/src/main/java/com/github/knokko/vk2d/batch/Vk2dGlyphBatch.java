package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.text.TextAlignment;
import com.github.knokko.vk2d.text.Vk2dFont;
import com.github.knokko.vk2d.text.Vk2dTextBuffer;

import java.nio.ByteBuffer;

import static java.lang.Math.max;

/**
 * This is the batch class of {@link com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline}. See the glyph pipeline docs
 * (link is in the README) for more information.
 */
public class Vk2dGlyphBatch extends Vk2dBatch {

	public final Vk2dTextBuffer textBuffer;
	public final long perFrameDescriptorSet;

	/**
	 * This method is for internal use only. Use {@link com.github.knokko.vk2d.pipeline.Vk2dGlyphPipeline#addBatch}
	 */
	public Vk2dGlyphBatch(
			Vk2dPipeline pipeline, Vk2dRenderStage frame, int initialCapacity,
			Vk2dTextBuffer textBuffer, long perFrameDescriptorSet
	) {
		super(pipeline, frame, initialCapacity);
		this.textBuffer = textBuffer;
		this.perFrameDescriptorSet = perFrameDescriptorSet;
	}

	public void glyphBetween(
			int minX, int minY, int maxX, int maxY,
			float subpixelX, float subpixelY, float width, float height,
			int horizontalIntersections, int verticalIntersections,
			int fillColor, int strokeColor, float strokeWidth
	) {
		if (horizontalIntersections == -1 || verticalIntersections == -1) return;
		if (maxX < 0 || maxY < 0 || minX >= this.width || minY >= this.height) return;

		MiniBatch triangles = putTriangles(2);

		ByteBuffer glyphInfo = triangles.vertexData()[1];
		int glyphByteOffset = Math.toIntExact(glyphInfo.position() + triangles.vertexBuffers()[1].offset - perFrameBuffer.buffer.offset);
		int glyphIndex = glyphByteOffset / (2 * pipeline.getBytesPerTriangle(1));

		// GlyphInfo.rawInfo
		glyphInfo.putInt(horizontalIntersections).putInt(verticalIntersections);
		putCompressedPosition(glyphInfo, minX, minY);
		glyphInfo.putInt(0);

		// GlyphInfo.colorsAndSize
		glyphInfo.putInt(1 + maxX - minX).putInt(1 + maxY - minY);
		glyphInfo.putInt(fillColor).putInt(strokeColor);

		// GlyphInfo.subpixelAndSize
		glyphInfo.putFloat(subpixelX).putFloat(subpixelY);
		glyphInfo.putFloat(width).putFloat(height);

		// GlyphInfo.strokeWidth
		glyphInfo.putFloat(strokeWidth);

		// GlyphInfo alignment padding
		glyphInfo.putInt(0).putInt(0).putInt(0);

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
			int fillColor, int strokeColor, float strokeWidth
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
		if (intMinX >= this.width || intMinY >= this.height || intBoundX <= 0 || intBoundY <= 0) return;

		float width = maxX - minX;
		float height = maxY - minY;
		int intWidth = intBoundX - intMinX;
		int intHeight = intBoundY - intMinY;
		float offsetX = minX - intMinX;
		float offsetY = intBoundY - maxY;

		int glyphOffsetHorizontal = textBuffer.scratch(
				font, glyph, -offsetY, heightA, height, intHeight, true, strokeWidth
		);
		int glyphOffsetVertical = textBuffer.scratch(
				font, glyph, -offsetX, heightA, width, intWidth, false, strokeWidth
		);

		glyphBetween(
				intMinX, intMinY, intBoundX - 1, intBoundY - 1, -offsetX, -offsetY, width, height,
				glyphOffsetHorizontal, glyphOffsetVertical,
				fillColor, strokeColor, strokeWidth
		);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA, Vk2dFont font, int fillColor
	) {
		drawString(text, baseX, baseY, heightA, font, fillColor, 0, 0f, TextAlignment.DEFAULT);
	}

	public void drawString(
			String text, int baseX, int baseY, int heightA,
			Vk2dFont font, int fillColor, TextAlignment alignment
	) {
		drawString(text, baseX, baseY, heightA, font, fillColor, 0, 0f, alignment);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA,
			Vk2dFont font, int fillColor, TextAlignment alignment
	) {
		drawString(text, baseX, baseY, heightA, font, fillColor, 0, 0f, alignment);
	}

	public void drawString(
			String text, int baseX, int baseY, int heightA, Vk2dFont font, int fillColor
	) {
		drawString(
				text, (float) baseX, (float) baseY, (float) heightA,
				font, fillColor, 0, 0f, TextAlignment.DEFAULT
		);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA,
			Vk2dFont font, int fillColor, int strokeColor, float strokeWidth, TextAlignment alignment
	) {
		// TODO CHAP2 Add HB support
		drawPrimitiveString(text, baseX, baseY, font, heightA, fillColor, strokeColor, strokeWidth, alignment);
	}

	public void drawString(
			String text, int baseX, int baseY, int heightA,
			Vk2dFont font, int fillColor, int strokeColor, float strokeWidth
	) {
		drawString(
				text, (float) baseX, (float) baseY, (float) heightA, font,
				fillColor, strokeColor, strokeWidth, TextAlignment.DEFAULT
		);
	}

	public void drawShadowedString(
			String text, float baseX, float baseY, float heightA,
			Vk2dFont font, int fillColor, int strokeColor, float strokeWidth,
			int shadowColor, float shadowOffsetX, float shadowOffsetY, TextAlignment alignment
	) {
		drawString(text, baseX + shadowOffsetX, baseY + shadowOffsetY, heightA, font, shadowColor, alignment);
		drawString(text, baseX, baseY, heightA, font, fillColor, strokeColor, strokeWidth, alignment);
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
		drawPrimitiveString(text, baseX, baseY, font, heightA, fillColor, strokeColor, strokeWidth, TextAlignment.LEFT);
	}

	public void drawPrimitiveString(
			String text, float baseX, float baseY, Vk2dFont font, float heightA,
			int fillColor, int strokeColor, float strokeWidth, TextAlignment alignment
	) {
		if (alignment == TextAlignment.DEFAULT) alignment = TextAlignment.LEFT;
		if (alignment == TextAlignment.REVERSED) alignment = TextAlignment.RIGHT;

		if (alignment != TextAlignment.LEFT) {
			float width = 0;
			for (int charIndex = 0; charIndex < text.length(); charIndex++) {
				char nextChar = text.charAt(charIndex);
				if (nextChar == '\t') {
					width += 4f * heightA * font.getWhitespaceAdvance();
				} else {
					int glyph = font.getGlyphForChar(nextChar);
					width += heightA * font.getGlyphAdvance(glyph);
				}
			}

			if (alignment == TextAlignment.CENTERED) baseX -= 0.5f * width;
			if (alignment == TextAlignment.RIGHT) baseX -= width;
		}

		for (int charIndex = 0; charIndex < text.length(); charIndex++) {
			char nextChar = text.charAt(charIndex);
			if (nextChar == '\t') {
				baseX += 4f * heightA * font.getWhitespaceAdvance();
				continue;
			}

			int glyph = font.getGlyphForChar(nextChar);
			glyphAt(baseX, baseY, font, heightA, glyph, fillColor, strokeColor, strokeWidth);

			baseX += heightA * font.getGlyphAdvance(glyph);
		}
	}
}
