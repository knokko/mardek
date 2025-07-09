package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

import java.nio.ByteBuffer;

import static com.github.knokko.boiler.utilities.ColorPacker.*;

public class Vk2dColorBatch extends Vk2dBatch<Vk2dColorPipeline> {

	public Vk2dColorBatch(Vk2dColorPipeline pipeline, PerFrameBuffer perFrameBuffer, int initialCapacity, int width, int height) {
		super(pipeline, perFrameBuffer, initialCapacity, width, height);
	}

	private void putColor(ByteBuffer vertices, int color) {
		vertices.putFloat(normalize(red(color))).putFloat(normalize(green(color)));
		vertices.putFloat(normalize(blue(color))).putFloat(normalize(alpha(color)));
	}

	public void fill(int minX, int minY, int maxX, int maxY, int color) {
		ByteBuffer vertices = putVertices(6);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY));
		putColor(vertices, color);
		vertices.putFloat(normalizeX(maxX)).putFloat(normalizeY(maxY));
		putColor(vertices, color);
		vertices.putFloat(normalizeX(maxX)).putFloat(normalizeY(minY));
		putColor(vertices, color);

		vertices.putFloat(normalizeX(maxX)).putFloat(normalizeY(minY));
		putColor(vertices, color);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		putColor(vertices, color);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY));
		putColor(vertices, color);
	}
}
