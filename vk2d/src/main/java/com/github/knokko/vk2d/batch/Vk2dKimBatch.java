package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;

import java.nio.ByteBuffer;

public class Vk2dKimBatch extends Vk2dBatch {

	public final Vk2dResourceBundle bundle;

	public Vk2dKimBatch(Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity, Vk2dResourceBundle bundle) {
		super(pipeline, frame, initialCapacity);
		this.bundle = bundle;
	}

	public void simple(int minX, int minY, int maxX, int maxY, int textureIndex) {
		int textureOffset = bundle.getFakeImageOffset(textureIndex);
		ByteBuffer vertices = putTriangles(2).vertexData()[0];

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
