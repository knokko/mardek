package com.github.knokko.ui.renderer.text;

public class GlyphKey {

	private static final int MASK3 = (1 << 3) - 1;
	private static final int MASK14 = (1 << 14) - 1;
	private static final int MASK24 = (1 << 24) - 1;

	public static int glyphIndex(long key) {
		return (int) (key) & MASK24;
	}

	public static int subpixelX(long key) {
		return (int) (key >> 24) & MASK3;
	}

	public static int subpixelY(long key) {
		return (int) (key >> 27) & MASK3;
	}

	public static int integerWidth(long key) {
		return (int) (key >> 30) & MASK14;
	}

	public static int subpixelWidth(long key) {
		return (int) (key >> 44) & MASK3;
	}

	public static int integerHeight(long key) {
		return (int) (key >> 47) & MASK14;
	}

	public static int subpixelHeight(long key) {
		return (int) (key >> 61) & MASK3;
	}
}
