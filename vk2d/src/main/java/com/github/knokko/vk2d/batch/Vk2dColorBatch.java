package com.github.knokko.vk2d.batch;

import com.github.knokko.boiler.buffers.PerFrameBuffer;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

import java.nio.ByteBuffer;

import static java.lang.Math.max;

public class Vk2dColorBatch extends Vk2dBatch<Vk2dColorPipeline> {

	public Vk2dColorBatch(
			Vk2dColorPipeline pipeline, PerFrameBuffer perFrameBuffer,
			int initialCapacity, int width, int height
	) {
		super(pipeline, perFrameBuffer, initialCapacity, width, height);
	}

	private void putPosition(ByteBuffer vertices, int x, int y) {
		vertices.putInt(max(x, 0) | (max(y, 0) << 16));
	}

	public void fill(int minX, int minY, int maxX, int maxY, int color) {
		ByteBuffer vertices = putVertices(6);
		putPosition(vertices, minX, maxY + 1);
		vertices.putInt(color);
		putPosition(vertices, maxX + 1, maxY + 1);
		vertices.putInt(color);
		putPosition(vertices, maxX + 1, minY);
		vertices.putInt(color);

		putPosition(vertices, maxX + 1, minY);
		vertices.putInt(color);
		putPosition(vertices, minX, minY);
		vertices.putInt(color);
		putPosition(vertices, minX, maxY + 1);
		vertices.putInt(color);
	}
}
