package com.github.knokko.vk2d.resource;

public class Vk2dFont {

	public final long vkDescriptorSet;
	private final int[] firstCurves, numCurves;
	private final float[] glyphWidths, glyphHeights;

	Vk2dFont(
			long vkDescriptorSet, int[] firstCurves, int[] numCurves,
			float[] glyphWidths, float[] glyphHeights
	) {
		this.vkDescriptorSet = vkDescriptorSet;
		this.firstCurves = firstCurves;
		this.numCurves = numCurves;
		this.glyphWidths = glyphWidths;
		this.glyphHeights = glyphHeights;
	}

	public int getFirstCurve(int glyph) {
		return firstCurves[glyph];
	}

	public int getNumCurves(int glyph) {
		return numCurves[glyph];
	}

	public float getGlyphWidth(int glyph) {
		return glyphWidths[glyph];
	}

	public float getGlyphHeight(int glyph) {
		return glyphHeights[glyph];
	}

	public int getNumGlyphs() {
		return glyphWidths.length;
	}
}
