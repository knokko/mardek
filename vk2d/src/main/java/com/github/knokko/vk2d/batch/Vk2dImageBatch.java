package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Vk2dImageBatch extends Vk2dBatch {

	public long[] descriptorSets;
	private int nextDescriptorIndex;

	public Vk2dImageBatch(Vk2dImagePipeline pipeline, Vk2dFrame frame, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
		this.descriptorSets = new long[initialCapacity];
	}

	public void simple(int minX, int minY, int maxX, int maxY, long descriptorSet) {
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
		descriptorSets[nextDescriptorIndex] = descriptorSet;
		nextDescriptorIndex += 1;
	}

	public void fillWithoutDistortion(
			int minX, int minY, int maxX, int maxY,
			long descriptorSet, int imageWidth, int imageHeight
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
		descriptorSets[nextDescriptorIndex] = descriptorSet;
		nextDescriptorIndex += 1;
	}
}
