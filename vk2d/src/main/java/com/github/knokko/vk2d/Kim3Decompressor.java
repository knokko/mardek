package com.github.knokko.vk2d;

import java.nio.ByteBuffer;

import static com.github.knokko.boiler.utilities.ColorPacker.linearToSrgb;

public class Kim3Decompressor {

	/**
	 * Extracts the width (in pixels) from the header (first element) of the compressed image data
	 */
	public static int width(int header) {
		return header & 4095;
	}

	/**
	 * Extracts the height (in pixels) from the header (first element) of the compressed image data
	 */
	public static int height(int header) {
		return (header >> 12) & 4095;
	}

	/**
	 * The width and height of the original image, in pixels
	 */
	public final int width, height;
	private final int numColors;
	private final ByteBuffer compressedData;

	/**
	 * Constructs a <i>Kim3Decompressor</i> that can recover the original image from the given <i>compressedData</i>
	 * @param compressedData The data that was the result of compressing an image using <i>Kim3Compressor</i>
	 */
	public Kim3Decompressor(ByteBuffer compressedData) {
		this.compressedData = compressedData;
		int header = compressedData.getInt(compressedData.position());

		this.width = width(header);
		this.height = height(header);
		this.numColors = (header >> 24) & 255;
	}

	/**
	 * Gets the color of the pixel at coordinates <i>(x, y)</i>, packed using the <i>ColorPacker</i> of vk-boiler.
	 * Note that this can lose some precision due to srgb <-> linear conversion</->
	 * @param x The x-coordinate of the pixel
	 * @param y The y-coordinate of the pixel
	 * @return The packed color of the pixel
	 */
	public int getColor(int x, int y) {
		int halfByteOffset = x + y * width;
		if (numColors > 16) halfByteOffset *= 2;
		int indirectIndex = compressedData.position() + 4 + 4 * numColors + 4 * (halfByteOffset / 8);
		int packedColorIndex = compressedData.getInt(indirectIndex);

		int colorIndex = packedColorIndex >> (4 * (halfByteOffset % 8));
		if (numColors > 16) colorIndex &= 255;
		else colorIndex &= 15;

		return linearToSrgb(compressedData.getInt(compressedData.position() + 4 + 4 * colorIndex));
	}
}
