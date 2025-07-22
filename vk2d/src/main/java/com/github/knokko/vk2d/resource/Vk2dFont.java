package com.github.knokko.vk2d.resource;

import java.util.Map;

public class Vk2dFont {

	public final long vkDescriptorSet;
	public final int index;
	public final int firstCurveIndex;
	private final int[] firstCurves, numCurves;
	private final float[] glyphMinX, glyphMinY, glyphMaxX, glyphMaxY;
	private final Map<Integer, Integer> charToGlyphMap;

	Vk2dFont(
			long vkDescriptorSet, int index, int firstCurveIndex, int[] firstCurves, int[] numCurves,
			float[] glyphMinX, float[] glyphMinY, float[] glyphMaxX, float[] glyphMaxY,
			Map<Integer, Integer> charToGlyphMap
	) {
		this.vkDescriptorSet = vkDescriptorSet;
		this.index = index;
		this.firstCurveIndex = firstCurveIndex;
		this.firstCurves = firstCurves;
		this.numCurves = numCurves;
		this.glyphMinX = glyphMinX;
		this.glyphMinY = glyphMinY;
		this.glyphMaxX = glyphMaxX;
		this.glyphMaxY = glyphMaxY;
		this.charToGlyphMap = charToGlyphMap;
	}

	public int getFirstCurve(int glyph) {
		return firstCurveIndex + firstCurves[glyph];
	}

	public int getGlyphForChar(int charCode) {
		return charToGlyphMap.getOrDefault(charCode, 0);
	}

	public int getNumCurves(int glyph) {
		return numCurves[glyph];
	}

	public float getGlyphMinX(int glyph) {
		return glyphMinX[glyph];
	}

	public float getGlyphMinY(int glyph) {
		return glyphMinY[glyph];
	}

	public float getGlyphMaxX(int glyph) {
		return glyphMaxX[glyph];
	}

	public float getGlyphMaxY(int glyph) {
		return glyphMaxY[glyph];
	}

	public int getNumGlyphs() {
		return glyphMinX.length;
	}
}
