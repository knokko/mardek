package com.github.knokko.vk2d;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.github.knokko.boiler.utilities.BoilerMath.nextMultipleOf;
import static com.github.knokko.boiler.utilities.ColorPacker.*;
import static java.lang.Math.max;

public class Kim3Compressor {

	public static int getWidth(int header) {
		return header & 4095;
	}

	public static int getHeight(int header) {
		return (header >> 12) & 4095;
	}

	/**
	 * The dimensions of the compressed image, in pixels
	 */
	public final int width, height;
	/**
	 * The size of the compressed image, in <b>int</b>s (4 <b>byte</b>s)
	 */
	public final int intSize;
	private final Map<Integer, Integer> colorTable = new HashMap<>();
	private final int[] pixelBuffer;

	/**
	 * Constructs a new <i>Kim1Compressor</i> capable of compressing the image stored in <i>pixelBuffer</i>.
	 * The <i>pixelBuffer</i> must have at least <b>width * height * 4</b> bytes <i>remaining()</i>. All pixels are
	 * expected to be stored in RGBA format.
	 *
	 * @param pixelBuffer The buffer that contains all the image data
	 * @param width The width of the image, in pixels
	 * @param height The height of the image, in pixels
	 * @throws IllegalArgumentException When a parameter is out of range, or the input is otherwise invalid
	 */
	public Kim3Compressor(ByteBuffer pixelBuffer, int width, int height) throws IllegalArgumentException {
		if (width >= 4096) throw new IllegalArgumentException("The width " + width + " must be smaller than 4096");
		if (height >= 4096) throw new IllegalArgumentException("The height " + height + " must be smaller than 4096");
		if (width < 1 || height < 1) throw new IllegalArgumentException("Both width and height must be positive");
		int expectedSize = 4 * width * height;
		if (expectedSize > pixelBuffer.remaining()) {
			throw new IllegalArgumentException("Expected imageData to have a length of at least " + expectedSize +
					", but got " + pixelBuffer.remaining());
		}
		this.width = width;
		this.height = height;

		this.pixelBuffer = new int[width * height];
		int bufferIndex = 0;

		while (pixelBuffer.hasRemaining()) {
			byte red = pixelBuffer.get();
			byte green = pixelBuffer.get();
			byte blue = pixelBuffer.get();
			byte alpha = pixelBuffer.get();
			int color = rgba(red, green, blue, alpha);
			this.pixelBuffer[bufferIndex] = color;
			bufferIndex += 1;
			if (!colorTable.containsKey(color)) colorTable.put(color, colorTable.size());
		}

		if (colorTable.size() >= 256) {
			throw new IllegalArgumentException(
					"Too many distinct colors: " + colorTable.size() + "; at most 255 are allowed"
			);
		}

		int size = 1 + colorTable.size();
		if (colorTable.size() > 16) size += nextMultipleOf(width * height, 4) / 4;
		else size += nextMultipleOf(width * height, 8) / 8;

		// A dirty, but effective, optimization in the vertex shader relies on the existence of at least 5 integers
		this.intSize = max(5, size);
	}

	/**
	 * Compresses the source image, and stores the result in <i>destination</i>. Note:
	 * <ul>
	 *     <li><i>destination</i> must have at least <b>this.intSize * 4</b> bytes <i>remaining()</i></li>
	 *     <li>this method will increase the <i>position()</i> of <i>destination</i></li>
	 * </ul>
	 */
	public void compress(ByteBuffer destination) {
		int oldPosition = destination.position();

		// Header
		destination.putInt(width | (height << 12) | (colorTable.size() << 24));

		// Color table
		var colorEntries = new ArrayList<>(colorTable.entrySet());
		colorEntries.sort(Comparator.comparingInt(Map.Entry::getValue));
		for (var entry : colorEntries) destination.putInt(srgbToLinear(entry.getKey()));

		// Color indices
		int nextValue = 0;
		int nextBitIndex = 0;
		int bitsPerPixel = colorTable.size() > 16 ? 8 : 4;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = pixelBuffer[x + width * y];
				int colorIndex = colorTable.get(color);
				nextValue |= colorIndex << nextBitIndex;
				nextBitIndex += bitsPerPixel;
				if (nextBitIndex == 32) {
					nextBitIndex = 0;
					destination.putInt(nextValue);
					nextValue = 0;
				}
			}
		}

		if (nextBitIndex != 0) destination.putInt(nextValue);

		// A dirty, but effective, optimization in the vertex shader relies on the existence of at least 5 integers
		while (destination.position() - oldPosition < 20) destination.putInt(0);
	}
}
