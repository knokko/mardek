package com.github.knokko.vk2d.resource;

import com.github.knokko.boiler.buffers.VkbBuffer;

public class Vk2dFont {

	public final long vkDescriptorSet;
	public final VkbBuffer curveBuffer;
	private final int[] firstCurves, numCurves;
	private final float[] glyphMinX, glyphMinY, glyphMaxX, glyphMaxY;

	Vk2dFont(
			long vkDescriptorSet, VkbBuffer curveBuffer, int[] firstCurves, int[] numCurves,
			float[] glyphMinX, float[] glyphMinY, float[] glyphMaxX, float[] glyphMaxY
	) {
		this.vkDescriptorSet = vkDescriptorSet;
		this.curveBuffer = curveBuffer;
		this.firstCurves = firstCurves;
		this.numCurves = numCurves;
		this.glyphMinX = glyphMinX;
		this.glyphMinY = glyphMinY;
		this.glyphMaxX = glyphMaxX;
		this.glyphMaxY = glyphMaxY;
	}

	public int getFirstCurve(int glyph) {
		// TODO Respect index for other fonts
		return firstCurves[glyph];
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
