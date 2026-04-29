package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dFancyTextPipeline;
import com.github.knokko.vk2d.text.*;
import com.github.knokko.vk2d.text.TextAlignment;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;

import static com.github.knokko.vk2d.text.HarfbuzzChecks.assertHbSuccess;
import static java.lang.Math.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_positions;

public class Vk2dFancyTextBatch extends Vk2dBatch {

	public SdfAtlas[] atlases;
	private int nextAtlasIndex;
	public final Vk2dFancyTextStyleCache cache;

	public Vk2dFancyTextBatch(
			Vk2dFancyTextPipeline pipeline, Vk2dRenderStage stage,
			int initialCapacity, Vk2dFancyTextStyleCache cache
	) {
		super(pipeline, stage, initialCapacity);
		this.atlases = new SdfAtlas[initialCapacity];
		this.cache = cache;
	}

	public void putGlyph(
			float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
			float baseX, float baseY, SdfAtlas atlas, float heightA, int glyph, int styleIndex
	) {
		var quad = putTriangles(2).vertexData()[0];
		quad.putFloat(normalizeX(x1)).putFloat(normalizeY(y1));
		quad.putFloat(normalizeX(x2)).putFloat(normalizeY(y2));
		quad.putFloat(normalizeX(x3)).putFloat(normalizeY(y3));
		quad.putFloat(normalizeX(x4)).putFloat(normalizeY(y4));

		putCompressedPosition(quad, atlas.getMinX(glyph), atlas.getMinY(glyph));
		quad.putFloat(atlas.getMaxU(glyph)).putFloat(atlas.getMaxV(glyph));

		quad.putFloat(heightA);
		quad.putFloat(normalizeX(baseX));
		quad.putFloat(normalizeY(baseY));
		quad.putInt(styleIndex);

		if (nextAtlasIndex >= atlases.length) {
			atlases = Arrays.copyOf(atlases, 2 * atlases.length);
		}
		atlases[nextAtlasIndex] = atlas;
		nextAtlasIndex += 1;
	}

	public void drawString(
			String text, float baseX, float baseY, float rotation, float heightA,
			Vk2dFont font, Vk2dFancyTextStyle style, TextAlignment alignment
	) {
		double radiansRotation = toRadians(rotation);
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

		float cosRotation = (float) cos(radiansRotation);
		float sinRotation = (float) sin(radiansRotation);
		float alignmentX = expectedWidth * cosRotation;
		float alignmentY = -expectedWidth * sinRotation;
		if (alignment == TextAlignment.LEFT && !leftToRight) {
			baseX += alignmentX;
			baseY += alignmentY;
		}
		if (alignment == TextAlignment.RIGHT && leftToRight) {
			baseX -= alignmentX;
			baseY -= alignmentY;
		}
		if (alignment == TextAlignment.CENTERED) {
			if (leftToRight) {
				baseX -= 0.5f * alignmentX;
				baseY -= 0.5f * alignmentY;
			} else {
				baseX += 0.5f * alignmentX;
				baseY += 0.5f * alignmentY;
			}
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
			var atlas = font.chooseAtlas(heightA, style.getEffectiveStrokeWidth(), glyphInfo.codepoint());

			int glyph = glyphInfo.codepoint();
			float minX = atlas.getRenderMinX(glyph, baseX, glyphOffset, heightA);
			float minY = atlas.getRenderMinY(glyph, baseY, glyphOffset, heightA);
			float maxX = atlas.getRenderMaxX(glyph, baseX, glyphOffset, heightA);
			float maxY = atlas.getRenderMaxY(glyph, baseY, glyphOffset, heightA);

			putGlyph(
					baseX + cosRotation * (minX - baseX) + sinRotation * (maxY - baseY),
					baseY + cosRotation * (maxY - baseY) - sinRotation * (minX - baseX),

					baseX + cosRotation * (maxX - baseX) + sinRotation * (maxY - baseY),
					baseY + cosRotation * (maxY - baseY) - sinRotation * (maxX - baseX),

					baseX + cosRotation * (maxX - baseX) + sinRotation * (minY - baseY),
					baseY + cosRotation * (minY - baseY) - sinRotation * (maxX - baseX),

					baseX + cosRotation * (minX - baseX) + sinRotation * (minY - baseY),
					baseY + cosRotation * (minY - baseY) - sinRotation * (minX - baseX),

					baseX, baseY, atlas, heightA, glyph, styleIndex
			);

			baseX += cosRotation * font.getGlyphAdvanceX(glyphOffset, heightA) - sinRotation * font.getGlyphAdvanceY(glyphOffset, heightA);
			baseY += -sinRotation * font.getGlyphAdvanceX(glyphOffset, heightA) + cosRotation * font.getGlyphAdvanceY(glyphOffset, heightA);
		}
	}

	public void drawShadowedString(
			String text, float baseX, float baseY, float rotation, float heightA, Vk2dFont font,
			Vk2dFancyTextStyle.Shadowed style, TextAlignment alignment
	) {
		float offset = style.shadowOffset() * heightA;
		float shadowX = baseX + offset;
		float shadowY = baseY + offset;
		drawString(text, shadowX, shadowY, rotation, heightA, font, style.shadowStyle(), alignment);
		drawString(text, baseX, baseY, rotation, heightA, font, style.mainStyle(), alignment);
	}
}
