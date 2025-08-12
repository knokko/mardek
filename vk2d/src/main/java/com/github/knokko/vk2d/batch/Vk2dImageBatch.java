package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.lang.Math.*;

public class Vk2dImageBatch extends Vk2dBatch {

	private final Vk2dResourceBundle bundle;
	public long[] descriptorSets;
	private int nextDescriptorIndex;

	public Vk2dImageBatch(Vk2dImagePipeline pipeline, Vk2dFrame frame, int initialCapacity, Vk2dResourceBundle bundle) {
		super(pipeline, frame, initialCapacity);
		this.bundle = bundle;
		this.descriptorSets = new long[initialCapacity];
	}

	public void simple(int minX, int minY, int maxX, int maxY, int imageIndex) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(1f);

		if (nextDescriptorIndex >= descriptorSets.length) descriptorSets = Arrays.copyOf(
				descriptorSets, 2 * descriptorSets.length
		);
		descriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(imageIndex);
		nextDescriptorIndex += 1;
	}

	public void simpleScale(int minX, int minY, float scale, int imageIndex) {
		int width = Math.round(scale * bundle.getImageWidth(imageIndex));
		int height = Math.round(scale * bundle.getImageHeight(imageIndex));
		simple(minX, minY, minX + width - 1, minY + height - 1, imageIndex);
	}

	public void fillWithoutDistortion(
			int minX, int minY, int maxX, int maxY,
			int imageIndex, int imageWidth, int imageHeight
	) {
		if (maxX < minX || maxY < minY) return;
		float imageAspectRatio = (float) imageWidth / imageHeight;
		float targetAspectRatio = (1f + maxX - minX) / (1f + maxY - minY);
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

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(minU).putFloat(maxV);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(maxU).putFloat(maxV);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(minV);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(minV);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(minU).putFloat(minV);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(minU).putFloat(maxV);

		if (nextDescriptorIndex >= descriptorSets.length) descriptorSets = Arrays.copyOf(
				descriptorSets, 2 * descriptorSets.length
		);
		descriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(imageIndex);
		nextDescriptorIndex += 1;
	}

	public void rotated(float midX, float midY, float angle, float scale, int imageIndex) {
		float hw = 0.5f * bundle.getImageWidth(imageIndex) * scale;
		float hh = 0.5f * bundle.getImageHeight(imageIndex) * scale;
		float rawAngle = (float) toRadians(angle);
		float sa = (float) sin(angle);
		float ca = (float) cos(angle);
		transformed(
				midX - hw * ca + hh * sa, midY + hh * ca + hw * sa,
				midX + hw * ca + hh * sa, midY + hh * ca - hw * sa,
				midX + hw * ca - hh * sa, midY - hh * ca - hw * sa,
				midX - hw * ca - hh * sa, midY - hh * ca + hw * sa,
				imageIndex
		);
	}

	public void transformed(
			float x1, float y1, float x2, float y2,
			float x3, float y3, float x4, float y4, int imageIndex
	) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];

		vertices.putFloat(normalizeX(x1)).putFloat(normalizeY(y1));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putFloat(normalizeX(x2)).putFloat(normalizeY(y2));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putFloat(normalizeX(x3)).putFloat(normalizeY(y3));
		vertices.putFloat(1f).putFloat(0f);

		vertices.putFloat(normalizeX(x3)).putFloat(normalizeY(y3));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putFloat(normalizeX(x4)).putFloat(normalizeY(y4));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putFloat(normalizeX(x1)).putFloat(normalizeY(y1));
		vertices.putFloat(0f).putFloat(1f);

		if (nextDescriptorIndex >= descriptorSets.length) descriptorSets = Arrays.copyOf(
				descriptorSets, 2 * descriptorSets.length
		);
		descriptorSets[nextDescriptorIndex] = bundle.getImageDescriptor(imageIndex);
		nextDescriptorIndex += 1;
	}
}
