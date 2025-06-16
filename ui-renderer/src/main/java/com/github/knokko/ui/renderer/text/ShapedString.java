package com.github.knokko.ui.renderer.text;

public class ShapedString {

	/**
	 * The X position of each glyph to be rendered. Use {@code x[i] / 8} to get the integer X of glyph {@code i},
	 * and use {@code x[i] % 8} to get the subpixel X of glyph {@code i}.
	 */
	public final int[] x;
	public final int[] y;

	/**
	 * The unique ID of the glyph that needs to be rendered at {@code Position(x[i], y[i])}
	 */
	public final int[] glyphID;

	/**
	 * The index of the {@code i}th glyph into the original string. This is usually not needed for rendering,
	 * but can be useful if a <i>part</i> of a string needs a different style or color.
	 */
	public final int[] charIndex;

	/**
	 * The number of frames since this entry was last used. Entries with a large {@code unusedFrames} should be
	 * removed to reclaim memory.
	 */
	public int unusedFrames = 0;

	public ShapedString(int[] x, int[] y, int[] glyphID, int[] charIndex) {
		this.x = x;
		this.y = y;
		this.glyphID = glyphID;
		this.charIndex = charIndex;
	}
}
