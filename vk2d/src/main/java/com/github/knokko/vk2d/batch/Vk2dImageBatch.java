package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.lang.Math.*;

/**
 * This is the batch class of {@link com.github.knokko.vk2d.pipeline.Vk2dImagePipeline}. See the image pipeline docs
 * (link is in the README) for more information.
 */
public class Vk2dImageBatch extends Vk2dBatch {

	private final Vk2dResourceBundle bundle;
	public long[] descriptorSets;
	private int nextDescriptorIndex;

	/**
	 * This method is for internal use only. Use {@link com.github.knokko.vk2d.pipeline.Vk2dImagePipeline#addBatch}
	 */
	public Vk2dImageBatch(Vk2dImagePipeline pipeline, Vk2dRenderStage stage, int initialCapacity, Vk2dResourceBundle bundle) {
		super(pipeline, stage, initialCapacity);
		this.bundle = bundle;
		this.descriptorSets = new long[initialCapacity];
	}

	public void simple(float minX, float minY, float boundX, float boundY, int imageIndex) {
		colored(minX, minY, boundX, boundY, imageIndex, 0, -1);
	}

	public void colored(
			float minX, float minY, float boundX, float boundY,
			int imageIndex, int addColor, int multiplyColor
	) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(boundY));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(boundY));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putInt(addColor).putInt(multiplyColor);

		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(boundY));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putInt(addColor).putInt(multiplyColor);

		if (nextDescriptorIndex >= descriptorSets.length) descriptorSets = Arrays.copyOf(
				descriptorSets, 2 * descriptorSets.length
		);
		descriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(imageIndex);
		nextDescriptorIndex += 1;
	}

	public void simpleScale(float minX, float minY, float scale, int imageIndex) {
		coloredScale(minX, minY, scale, imageIndex, 0, -1);
	}

	public void coloredScale(float minX, float minY, float scale, int imageIndex, int addColor, int multiplyColor) {
		float width = scale * bundle.getImageWidth(imageIndex);
		float height = scale * bundle.getImageHeight(imageIndex);
		colored(minX, minY, minX + width, minY + height, imageIndex, addColor, multiplyColor);
	}

	public void fillWithoutDistortion(float minX, float minY, float boundX, float boundY, int imageIndex) {
		if (boundX <= minX || boundY <= minY) return;
		int imageWidth = bundle.getImageWidth(imageIndex);
		int imageHeight = bundle.getImageHeight(imageIndex);
		float imageAspectRatio = (float) imageWidth / imageHeight;
		float targetAspectRatio = (boundX - minX) / (boundY - minY);
		float minU, minV, maxU, maxV;

		float relativeAspect = imageAspectRatio / targetAspectRatio;
		if (relativeAspect >= 1f) {
			// The image is wider than the target region, so we discard the left part & right part of the image
			minV = 0f;
			maxV = 1f;

			// relativeAspect = 1.0 -> minU = 0.0 = 0.5 - 1 / 2
			// relativeAspect = 2.0 -> minU = 0.25 = 0.5 - 1 / 4
			// relativeAspect = 3.0 -> minU = 0.33 = 0.5 - 1 / 6
			// relativeAspect = 4.0 -> minU = 0.375 = 0.5 - 1 / 8
			minU = 0.5f - 1f / (2f * relativeAspect);
			maxU = 1f - minU;
		} else {
			// The image is taller than the target region, so we discard the bottom part & upper part of the image
			minU = 0f;
			maxU = 1f;

			// Use the same formula as when relativeAspect >= 1f
			relativeAspect = 1f / relativeAspect;
			minV = 0.5f - 1f / (2f * relativeAspect);
			maxV = 1f - minV;
		}

		ByteBuffer vertices = putTriangles(2).vertexData()[0];

		int addColor = 0;
		int multiplyColor = -1;
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(boundY));
		vertices.putFloat(minU).putFloat(maxV);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(boundY));
		vertices.putFloat(maxU).putFloat(maxV);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(minV);
		vertices.putInt(addColor).putInt(multiplyColor);

		vertices.putFloat(normalizeX(boundX)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(minV);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(minU).putFloat(minV);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(boundY));
		vertices.putFloat(minU).putFloat(maxV);
		vertices.putInt(addColor).putInt(multiplyColor);

		if (nextDescriptorIndex >= descriptorSets.length) descriptorSets = Arrays.copyOf(
				descriptorSets, 2 * descriptorSets.length
		);
		descriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(imageIndex);
		nextDescriptorIndex += 1;
	}

	public void rotated(
			float midX, float midY, float angle, float scale,
			int imageIndex, int addColor, int multiplyColor
	) {
		float hw = 0.5f * bundle.getImageWidth(imageIndex) * scale;
		float hh = 0.5f * bundle.getImageHeight(imageIndex) * scale;
		float rawAngle = (float) toRadians(angle);
		float sa = (float) sin(rawAngle);
		float ca = (float) cos(rawAngle);
		transformed(
				midX - hw * ca + hh * sa, midY + hh * ca + hw * sa,
				midX + hw * ca + hh * sa, midY + hh * ca - hw * sa,
				midX + hw * ca - hh * sa, midY - hh * ca - hw * sa,
				midX - hw * ca - hh * sa, midY - hh * ca + hw * sa,
				imageIndex, addColor, multiplyColor
		);
	}

	public void transformed(
			float x1, float y1, float x2, float y2,
			float x3, float y3, float x4, float y4,
			int imageIndex, int addColor, int multiplyColor
	) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];

		vertices.putFloat(normalizeX(x1)).putFloat(normalizeY(y1));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(x2)).putFloat(normalizeY(y2));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(x3)).putFloat(normalizeY(y3));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putInt(addColor).putInt(multiplyColor);

		vertices.putFloat(normalizeX(x3)).putFloat(normalizeY(y3));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(x4)).putFloat(normalizeY(y4));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putInt(addColor).putInt(multiplyColor);
		vertices.putFloat(normalizeX(x1)).putFloat(normalizeY(y1));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putInt(addColor).putInt(multiplyColor);

		if (nextDescriptorIndex >= descriptorSets.length) descriptorSets = Arrays.copyOf(
				descriptorSets, 2 * descriptorSets.length
		);
		descriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(imageIndex);
		nextDescriptorIndex += 1;
	}
}
