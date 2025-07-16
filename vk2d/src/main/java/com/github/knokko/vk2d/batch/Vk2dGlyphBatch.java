package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.nio.ByteBuffer;

public class Vk2dGlyphBatch extends Vk2dBatch {

	public final long descriptorSet;

	public Vk2dGlyphBatch(Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity, long descriptorSet) {
		super(pipeline, frame, initialCapacity);
		this.descriptorSet = descriptorSet;
	}

	public void simple(int minX, int minY, int maxX, int maxY, int baseIndex) {
		ByteBuffer vertices = putVertices(6);

		int height = 1 + maxY - minY;
		float minU = -0.0f;
		float maxU = 1.0f;
		// TODO More accurate texture coordinates
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(minU).putFloat(0f).putInt(baseIndex).putInt(height);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(maxU).putFloat(0f).putInt(baseIndex).putInt(height);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(1f).putInt(baseIndex).putInt(height);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(maxU).putFloat(1f).putInt(baseIndex).putInt(height);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(minU).putFloat(1f).putInt(baseIndex).putInt(height);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(minU).putFloat(0f).putInt(baseIndex).putInt(height);
	}
}
