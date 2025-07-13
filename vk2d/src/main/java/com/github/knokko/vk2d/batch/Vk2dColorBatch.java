package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

import java.nio.ByteBuffer;

import static java.lang.Math.max;

public class Vk2dColorBatch extends Vk2dBatch {

	public Vk2dColorBatch(Vk2dColorPipeline pipeline, Vk2dFrame frame, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
	}

	private void putPosition(ByteBuffer vertices, int x, int y) {
		vertices.putInt(max(x, 0) | (max(y, 0) << 16));
	}

	public void fill(int minX, int minY, int maxX, int maxY, int color) {
		fillUnaligned(minX, maxY + 1, maxX + 1, maxY + 1, maxX + 1, minY, minX, minY, color);
	}

	public void fillUnaligned(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, int color) {
		ByteBuffer vertices = putVertices(6);
		putPosition(vertices, x1, y1);
		vertices.putInt(color);
		putPosition(vertices, x2, y2);
		vertices.putInt(color);
		putPosition(vertices, x3, y3);
		vertices.putInt(color);

		putPosition(vertices, x3, y3);
		vertices.putInt(color);
		putPosition(vertices, x4, y4);
		vertices.putInt(color);
		putPosition(vertices, x1, y1);
		vertices.putInt(color);
	}
}
