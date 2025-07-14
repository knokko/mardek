package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.resource.Vk2dFont;

import java.nio.ByteBuffer;

public class Vk2dTextBatch extends Vk2dBatch {

	public final Vk2dFont font;

	public Vk2dTextBatch(Vk2dPipeline pipeline, Vk2dFrame frame, int initialCapacity, Vk2dFont font) {
		super(pipeline, frame, initialCapacity);
		this.font = font;
	}

	public void simple(int minX, int minY, int maxX, int maxY, int glyph) {
		ByteBuffer vertices = putVertices(6);

		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(1f).putInt(glyph);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(1f).putFloat(1f).putInt(glyph);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f).putInt(glyph);

		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.putFloat(1f).putFloat(0f).putInt(glyph);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.putFloat(0f).putFloat(0f).putInt(glyph);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.putFloat(0f).putFloat(1f).putInt(glyph);
	}
}
