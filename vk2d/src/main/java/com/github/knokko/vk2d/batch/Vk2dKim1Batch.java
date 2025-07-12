package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.vk2d.pipeline.Vk2dKim1Pipeline;

import java.nio.ByteBuffer;

public class Vk2dKim1Batch extends Vk2dBatch<Vk2dKim1Pipeline> {

	public Vk2dKim1Batch(
			Vk2dKim1Pipeline pipeline, PerFrameBuffer perFrameBuffer,
			int initialCapacity, int width, int height
	) {
		super(pipeline, perFrameBuffer, initialCapacity, width, height);
	}

	public void simple(int minX, int minY, int maxX, int maxY, int textureOffset) {
		ByteBuffer vertices = putVertices(6);

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(1f).putInt(textureOffset);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(1f).putFloat(1f).putInt(textureOffset);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f).putInt(textureOffset);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f).putInt(textureOffset);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(0f).putInt(textureOffset);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(1f).putInt(textureOffset);
	}
}
