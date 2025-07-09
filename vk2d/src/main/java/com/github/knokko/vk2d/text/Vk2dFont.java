package com.github.knokko.vk2d.text;

import java.util.Map;

public class Vk2dFont {

	public final long vkDescriptorSet;
	public final int index;
	public final int firstCurveIndex;
	private final int[] firstCurves, numCurves;
	private final float[] glyphMinX, glyphMinY, glyphMaxX, glyphMaxY, glyphAdvance;
	private final Map<Integer, Integer> charToGlyphMap;
	private final float whitespaceAdvance;

	public Vk2dFont(
			long vkDescriptorSet, int index, int firstCurveIndex, int[] firstCurves, int[] numCurves,
			float[] glyphMinX, float[] glyphMinY, float[] glyphMaxX, float[] glyphMaxY, float[] glyphAdvance,
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
		this.glyphAdvance = glyphAdvance;
		this.charToGlyphMap = charToGlyphMap;
		int whitespaceGlyph = charToGlyphMap.get((int) ' ');
		this.whitespaceAdvance = glyphAdvance[whitespaceGlyph];
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

	public float getGlyphAdvance(int glyph) {
		return glyphAdvance[glyph];
	}

	public float getWhitespaceAdvance() {
		return whitespaceAdvance;
	}

	public int getNumGlyphs() {
		return glyphMinX.length;
	}
}
