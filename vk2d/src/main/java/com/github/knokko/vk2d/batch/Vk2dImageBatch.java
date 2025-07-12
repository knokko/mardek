package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.vk2d.pipeline.Vk2dImagePipeline;

import java.nio.ByteBuffer;

public class Vk2dImageBatch extends Vk2dBatch<Vk2dImagePipeline> {

	public Vk2dImageBatch(
			Vk2dImagePipeline pipeline, PerFrameBuffer perFrameBuffer,
			int initialCapacity, int width, int height
	) {
		super(pipeline, perFrameBuffer, initialCapacity, width, height);
	}

	public void simple(int minX, int minY, int maxX, int maxY, long descriptorSet) {
		ByteBuffer vertices = putVertices(7);

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(0f);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(1f).putFloat(0f);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(1f);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(1f);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(1f);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(0f);

		vertices.putLong(descriptorSet);
		vertices.putLong(0); // Each vertex is 16 bytes, so we need 8 bytes descriptor + 8 bytes padding
	}
}
