package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Vk2dImageBatch extends Vk2dBatch<Vk2dImagePipeline> {

	public long[] descriptorSets;
	private int nextDescriptorIndex;

	public Vk2dImageBatch(
			Vk2dImagePipeline pipeline, PerFrameBuffer perFrameBuffer,
			int initialCapacity, int width, int height
	) {
		super(pipeline, perFrameBuffer, initialCapacity, width, height);
		this.descriptorSets = new long[initialCapacity];
	}

	public void simple(int minX, int minY, int maxX, int maxY, long descriptorSet) {
		ByteBuffer vertices = putVertices(6);

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
}
