package com.github.knokko.vk2d.resource;

import com.github.knokko.vk2d.text.Vk2dFont;

public class Vk2dResourceBundle {

	private final long[] imageDescriptorSets;
	private final int[] imageWidths;
	private final int[] imageHeights;
	public final int numImages;

	private final Vk2dFont[] fonts;

	public final long fakeImageDescriptorSet;
	private final int[] fakeImageOffsets;
	private final int[] fakeImageWidths;
	private final int[] fakeImageHeights;
	public final int numFakeImages;

	Vk2dResourceBundle(
			long[] imageDescriptorSets, int[] imageWidths, int[] imageHeights, Vk2dFont[] fonts,
			long fakeImageDescriptorSet, int[] fakeImageOffsets, int[] fakeImageWidths, int[] fakeImageHeights
	) {
		this.imageDescriptorSets = imageDescriptorSets;
		this.numImages = imageDescriptorSets.length;
		this.imageWidths = imageWidths;
		this.imageHeights = imageHeights;
		this.fonts = fonts;

		this.fakeImageDescriptorSet = fakeImageDescriptorSet;
		this.fakeImageOffsets = fakeImageOffsets;
		this.fakeImageWidths = fakeImageWidths;
		this.fakeImageHeights = fakeImageHeights;
		this.numFakeImages = fakeImageHeights.length;
	}

	public long getImageDescriptor(int imageIndex) {
		return imageDescriptorSets[imageIndex];
	}

	public int getImageWidth(int imageIndex) {
		return imageWidths[imageIndex];
	}

	public int getImageHeight(int imageIndex) {
		return imageHeights[imageIndex];
	}

	public Vk2dFont getFont(int fontIndex) {
		return fonts[fontIndex];
	}

	public int getNumFonts() {
		return fonts.length;
	}

	public int getFakeImageOffset(int index) {
		return fakeImageOffsets[index];
	}

	public int getFakeImageWidth(int index) {
		return fakeImageWidths[index];
	}

	public int getFakeImageHeight(int index) {
		return fakeImageHeights[index];
	}
}
