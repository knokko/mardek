package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dSimpleTextPipeline;
import com.github.knokko.vk2d.text.SdfAtlas;
import com.github.knokko.vk2d.text.Vk2dFont;
import com.github.knokko.vk2d.text.Vk2dTextStyle;
import com.github.knokko.vk2d.text.Vk2dTextStyleCache;
import com.github.knokko.vk2d.text.TextAlignment;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.harfbuzz.hb_glyph_position_t;

import java.util.Arrays;

import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static java.lang.Math.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

public class Vk2dSimpleTextBatch extends Vk2dBatch {

	public SdfAtlas[] atlases;
	private int nextAtlasIndex;
	public final Vk2dTextStyleCache cache;

	public Vk2dSimpleTextBatch(
			Vk2dSimpleTextPipeline pipeline, Vk2dRenderStage stage,
			int initialCapacity, Vk2dTextStyleCache cache
	) {
		super(pipeline, stage, initialCapacity);
		this.atlases = new SdfAtlas[initialCapacity];
		this.cache = cache;
	}

	public void glyphAt(
			float baseX, float baseY, hb_glyph_position_t glyphOffset, SdfAtlas atlas,
			float heightA, int glyph, int styleIndex
	) {
		float minX = normalizeX(atlas.getRenderMinX(glyph, baseX, glyphOffset, heightA));
		float minY = normalizeY(atlas.getRenderMinY(glyph, baseY, glyphOffset, heightA));
		float maxX = normalizeX(atlas.getRenderMaxX(glyph, baseX, glyphOffset, heightA));
		float maxY = normalizeY(atlas.getRenderMaxY(glyph, baseY, glyphOffset, heightA));

		int minU = atlas.getMinX(glyph);
		int minV = atlas.getMinY(glyph);
		float maxU = atlas.getMaxU(glyph);
		float maxV = atlas.getMaxV(glyph);

		var quad = putTriangles(2).vertexData()[0];

		quad.putFloat(minX).putFloat(maxY);
		quad.putFloat(maxX).putFloat(minY);

		putCompressedPosition(quad, minU, minV);
		quad.putFloat(maxU).putFloat(maxV);

		quad.putFloat(heightA);
		quad.putInt(styleIndex);

		if (nextAtlasIndex >= atlases.length) {
			atlases = Arrays.copyOf(atlases, 2 * atlases.length);
		}
		atlases[nextAtlasIndex] = atlas;
		nextAtlasIndex += 1;
	}

	public void drawString(String text, float baseX, float baseY, float heightA, Vk2dFont font, int fillColor) {
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

	public void drawString(String text, int baseX, int baseY, int heightA, Vk2dFont font, int fillColor) {
		drawString(
				text, (float) baseX, (float) baseY, (float) heightA,
				font, fillColor, 0, 0f, TextAlignment.DEFAULT
		);
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
			int shadowColor, float shadowOffset, TextAlignment alignment
	) {
		drawString(text, baseX + shadowOffset, baseY + shadowOffset, heightA, font, shadowColor, alignment);
		drawString(text, baseX, baseY, heightA, font, fillColor, strokeColor, strokeWidth, alignment);
	}

	public void drawShadowedString(
			String text, float baseX, float baseY, float heightA, Vk2dFont font,
			Vk2dTextStyle.Shadowed style, TextAlignment alignment
	) {
		float offset = style.shadowOffset() * heightA;
		drawString(text, baseX + offset, baseY + offset, heightA, font, style.shadowStyle(), alignment);
		drawString(text, baseX, baseY, heightA, font, style.mainStyle(), alignment);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA, Vk2dFont font,
			int fillColor, int strokeColor, float strokeWidth, TextAlignment alignment
	) {
		Vk2dTextStyle style = new Vk2dTextStyle(
				new Vk2dTextStyle.FillStyle(fillColor),
				new Vk2dTextStyle.StrokeStyle(strokeColor, strokeWidth / heightA, true, 0.5f)
		);
		drawString(text, baseX, baseY, heightA, font, style, alignment);
	}

	public void drawString(
			String text, float baseX, float baseY, float heightA,
			Vk2dFont font, Vk2dTextStyle style, TextAlignment alignment
	) {
		hb_buffer_clear_contents(cache.hbBuffer);

		try (var stack = MemoryStack.stackPush()) {
			var textBytes = stack.UTF16(text, false);
			hb_buffer_add_utf16(cache.hbBuffer, textBytes, 0, textBytes.capacity());
		}

		hb_buffer_guess_segment_properties(cache.hbBuffer);
		hb_shape(font.hbFont, cache.hbBuffer, null);

		var glyphInfos = assertHbSuccess(
				hb_buffer_get_glyph_infos(cache.hbBuffer),
				"buffer_get_glyph_infos"
		);
		var glyphOffsets = assertHbSuccess(
				hb_buffer_get_glyph_positions(cache.hbBuffer),
				"buffer_get_glyph_positions"
		);

		float testX = baseX;
		for (int index = glyphInfos.position(); index < glyphInfos.limit(); index++) {
			var glyphInfo = glyphInfos.get(index);
			int charIndex = glyphInfo.cluster();
			if (charIndex < text.length() && text.charAt(charIndex) == '\t') {
				testX += 4 * font.getWhitespaceAdvance(heightA);
			} else {
				testX += font.getGlyphAdvanceX(glyphOffsets.get(index), heightA);
			}
		}

		float leftBoundary = min(baseX, testX);
		float rightBoundary = max(baseX, testX);
		float expectedWidth = rightBoundary - leftBoundary;
		boolean leftToRight = baseX == leftBoundary;

		if (alignment == TextAlignment.DEFAULT) {
			alignment = leftToRight ? TextAlignment.LEFT : TextAlignment.RIGHT;
		}
		if (alignment == TextAlignment.REVERSED) {
			alignment = leftToRight ? TextAlignment.RIGHT : TextAlignment.LEFT;
		}

		if (alignment == TextAlignment.LEFT && !leftToRight) baseX += expectedWidth;
		if (alignment == TextAlignment.RIGHT && leftToRight) baseX -= expectedWidth;
		if (alignment == TextAlignment.CENTERED) {
			if (leftToRight) baseX -= expectedWidth * 0.5f;
			else baseX += expectedWidth * 0.5f;
		}

		int styleIndex = cache.getStyleIndex(style);
		for (int index = glyphInfos.position(); index < glyphInfos.limit(); index++) {
			var glyphInfo = glyphInfos.get(index);
			int charIndex = glyphInfo.cluster();
			if (charIndex < text.length() && text.charAt(charIndex) == '\t') {
				baseX += 4 * font.getWhitespaceAdvance(heightA);
				continue;
			}

			var glyphOffset = glyphOffsets.get(index);
			var atlas = font.chooseAtlas(heightA, style.stroke().width(), glyphInfo.codepoint());
			glyphAt(baseX, baseY, glyphOffset, atlas, heightA, glyphInfo.codepoint(), styleIndex);

			baseX += font.getGlyphAdvanceX(glyphOffset, heightA);
			baseY += font.getGlyphAdvanceY(glyphOffset, heightA);
		}
	}
}
