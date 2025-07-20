package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.resource.Vk2dFont;

import java.nio.ByteBuffer;

public class Vk2dGlyphBatch extends Vk2dBatch {

	private final Vk2dFont font;
	public final long descriptorSet;

	public Vk2dGlyphBatch(Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity, Vk2dFont font, long descriptorSet) {
		super(pipeline, frame, initialCapacity);
		this.font = font;
		this.descriptorSet = descriptorSet;
	}

	public void glyphBetween(
			int minX, int minY, int maxX, int maxY,
			int horizontalIntersections, int verticalIntersections
	) {
		if (horizontalIntersections == -1 || verticalIntersections == -1) return;
		ByteBuffer vertices = putVertices(6);

		int width = 1 + maxX - minX;
		int height = 1 + maxY - minY;
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(horizontalIntersections).putInt(verticalIntersections).putInt(width).putInt(height);
	}

	public int determineWidth(float heightA, int glyph) {
		return (int) (heightA * (font.getGlyphMaxX(glyph) - font.getGlyphMinX(glyph)));
	}

	public int determineHeight(float heightA, int glyph) {
		return (int) (heightA * (font.getGlyphMaxY(glyph) - font.getGlyphMinY(glyph)));
	}

	public void glyphAt(
			float baseX, float baseY, float heightA, int glyph,
			int horizontalIntersections, int verticalIntersections
	) {
		if (font.getNumCurves(glyph) == 0) return;
		int minX = (int) (baseX + heightA * font.getGlyphMinX(glyph));
		int minY = (int) (baseY - heightA * font.getGlyphMaxY(glyph));
		glyphBetween(
				minX, minY,
				minX + determineWidth(heightA, glyph) - 1,
				minY + determineHeight(heightA, glyph) - 1,
				horizontalIntersections, verticalIntersections
		);
	}
}
