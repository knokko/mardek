package com.github.knokko.vk2d.resource;

/**
 * Given an image that should be interpreted as greyscale image, this enum determines how the greyscale value should
 * be extracted from each pixel.
 */
public enum Vk2dGreyscaleChannel {
	/**
	 * The greyscale value is the average of the red, green, and blue components.
	 */
	RGB,
	/**
	 * The greyscale value is the alpha value
	 */
	ALPHA,
	/**
	 * The greyscale value is the red value
	 */
	RED
}
