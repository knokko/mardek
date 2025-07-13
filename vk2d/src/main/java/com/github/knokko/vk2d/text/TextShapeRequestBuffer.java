package com.github.knokko.vk2d.text;

import com.github.knokko.text.placement.TextAlignment;
import org.lwjgl.util.freetype.FT_Face;

public class TextShapeRequestBuffer {

	private static final int INITIAL_CAPACITY = 100;

	private FT_Face[] font = new FT_Face[INITIAL_CAPACITY];
	private String[] text = new String[INITIAL_CAPACITY];
	private int[] color = new int[INITIAL_CAPACITY];
	private int[] outlineColorIndex = new int[INITIAL_CAPACITY];
	private int[] numOutlineColors = new int[INITIAL_CAPACITY];
	private int[] minX = new int[INITIAL_CAPACITY];
	private int[] minY = new int[INITIAL_CAPACITY];
	private int[] maxX = new int[INITIAL_CAPACITY];
	private int[] maxY = new int[INITIAL_CAPACITY];
	private int[] baseY = new int[INITIAL_CAPACITY];
	private int[] heightA = new int[INITIAL_CAPACITY];
	private TextAlignment[] alignment = new TextAlignment[INITIAL_CAPACITY];
	private int[] gradientIndex = new int[INITIAL_CAPACITY];
	private int[] numGradients = new int[INITIAL_CAPACITY];
}
