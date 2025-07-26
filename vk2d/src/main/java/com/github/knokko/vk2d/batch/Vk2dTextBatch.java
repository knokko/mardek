package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.text.Vk2dFont;

import java.nio.ByteBuffer;

public class Vk2dTextBatch extends Vk2dBatch {

	public final Vk2dFont font;

	public Vk2dTextBatch(Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity, Vk2dFont font) {
		super(pipeline, frame, initialCapacity);
		this.font = font;
	}

	public void glyphBetween(
			float minX, float minY, float boundX, float boundY,
			float minU, float minV, float maxU, float maxV, int glyph, int color
	) {
		ByteBuffer vertices = putVertices(6);

		int firstCurve = font.getFirstCurve(glyph);
		int numCurves = font.getNumCurves(glyph);

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(boundY));
		vertices.putFloat(minU).putFloat(minV);
		vertices.putInt(firstCurve).putInt(numCurves).putInt(color);
		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(boundY));
		vertices.putFloat(maxU).putFloat(minV);
		vertices.putInt(firstCurve).putInt(numCurves).putInt(color);
		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(maxV);
		vertices.putInt(firstCurve).putInt(numCurves).putInt(color);

		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(maxV);
		vertices.putInt(firstCurve).putInt(numCurves).putInt(color);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(minU).putFloat(maxV);
		vertices.putInt(firstCurve).putInt(numCurves).putInt(color);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(boundY));
		vertices.putFloat(minU).putFloat(minV);
		vertices.putInt(firstCurve).putInt(numCurves).putInt(color);
	}

	public void glyphAt(float baseX, float baseY, float heightA, int glyph, int color) {
		glyphBetween(
				baseX + heightA * font.getGlyphMinX(glyph),
				baseY - heightA * font.getGlyphMaxY(glyph),
				baseX + heightA * font.getGlyphMaxX(glyph),
				baseY - heightA * font.getGlyphMinY(glyph),
				font.getGlyphMinX(glyph),
				font.getGlyphMinY(glyph),
				font.getGlyphMaxX(glyph),
				font.getGlyphMaxY(glyph),
				glyph, color
		);
	}
}
