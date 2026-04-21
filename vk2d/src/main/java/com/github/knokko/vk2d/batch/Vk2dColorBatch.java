package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

/**
 * This is the batch class of {@link com.github.knokko.vk2d.pipeline.Vk2dColorPipeline}. See the color pipeline docs
 * (link is in the README) for more information.
 */
public class Vk2dColorBatch extends Vk2dAbstractColorBatch {

	/**
	 * This constructor is for internal use only. Use {@link com.github.knokko.vk2d.pipeline.Vk2dColorPipeline#addBatch}
	 */
	public Vk2dColorBatch(Vk2dColorPipeline pipeline, Vk2dRenderStage frame, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
	}

	/**
	 * Fills the axis-aligned rectangle between (minX, minY) and (maxX, maxY) with {@code color}.
	 * All coordinates are inclusive.
	 * @param minX The minimum (left-most) X-coordinate, inclusive
	 * @param minY The minimum (top-most) Y-coordinate, inclusive
	 * @param maxX The maximum (right-most) X-coordinate, inclusive
	 * @param maxY The maximum (bottom-most) Y-coordinate, inclusive
	 * @param color The fill color
	 */
	@Override
	public void fill(int minX, int minY, int maxX, int maxY, int color) {
		super.fill(minX, minY, maxX, maxY, color);
	}

	/**
	 * Fills the quad with corners (x1, y1), (x2, y2), (x3, y3), and (x4, y4) with {@code color}.
	 * Note that the top-left of each corner pixel is used, so choosing the corners (0, 0), (1, 0), (1, 1), and (0, 1)
	 * would cause only the pixel at (0, 0) to be filled with {@code color}.
	 * @param x1 The X-coordinate of the first corner
	 * @param y1 The Y-coordinate of the first corner
	 * @param x2 The X-coordinate of the second corner
	 * @param y2 The Y-coordinate of the second corner
	 * @param x3 The X-coordinate of the third corner
	 * @param y3 The Y-coordinate of the third corner
	 * @param x4 The X-coordinate of the fourth corner
	 * @param y4 The Y-coordinate of the fourth corner
	 * @param color The fill color of the quad
	 */
	@Override
	public void fillUnaligned(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, int color) {
		super.fillUnaligned(x1, y1, x2, y2, x3, y3, x4, y4, color);
	}
}
