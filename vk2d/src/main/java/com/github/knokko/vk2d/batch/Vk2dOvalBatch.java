package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dOvalPipeline;

import java.nio.ByteBuffer;

public class Vk2dOvalBatch extends Vk2dBatch {

	public Vk2dOvalBatch(Vk2dOvalPipeline pipeline, Vk2dFrame frame, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
	}

	public void complex(
			int minX, int minY, int maxX, int maxY,
			int centerX, int centerY, int radiusX, int radiusY,
			int centerColor, int color0, int color1, int color2, int color3,
			float distance0, float distance1, float distance2, float distance3
	) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];
		int startPosition = vertices.position();

		for (int counter = 0; counter < 6; counter++) {
			vertices.putInt(0).putInt(0);
			vertices.putFloat(normalizeX(centerX)).putFloat(normalizeY(centerY));
			vertices.putFloat(2f * radiusX / width).putFloat(2f * radiusY / height);
			vertices.putInt(centerColor).putInt(color0).putInt(color1).putInt(color2).putInt(color3);
			vertices.putFloat(distance0 * distance0).putFloat(distance1 * distance1);
			vertices.putFloat(distance2 * distance2).putFloat(distance3 * distance3);
		}

		vertices.position(startPosition);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.position(startPosition + 60);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(maxY + 1));
		vertices.position(startPosition + 120);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));

		vertices.position(startPosition + 180);
		vertices.putFloat(normalizeX(maxX + 1)).putFloat(normalizeY(minY));
		vertices.position(startPosition + 240);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(minY));
		vertices.position(startPosition + 300);
		vertices.putFloat(normalizeX(minX)).putFloat(normalizeY(maxY + 1));
		vertices.position(startPosition + 360);
	}

	public void simpleAliased(int minX, int minY, int maxX, int maxY, int color) {
		int centerX = (minX + maxX) / 2;
		int centerY = (minY + maxY) / 2;
		aliased(minX, minY, maxX, maxY, centerX, centerY, centerX - minX, centerY - minY, color);
	}

	public void aliased(
			int minX, int minY, int maxX, int maxY,
			int centerX, int centerY, int radiusX, int radiusY, int color
	) {
		complex(
				minX, minY, maxX, maxY, centerX, centerY, radiusX, radiusY,
				color, color, 0, 0, 0,
				1f, 1f, 100f, 100f
		);
	}

	public void simpleAntiAliased(int minX, int minY, int maxX, int maxY, float fade, int color) {
		int centerX = (minX + maxX) / 2;
		int centerY = (minY + maxY) / 2;
		int radiusX = centerX - minX;
		int radiusY = centerY - minY;
		int marginX = 1 + (int) (fade * radiusX);
		int marginY = 1 + (int) (fade * radiusY);
		antiAliased(
				minX - marginX, minY - marginY, maxX + marginX, maxY + marginY,
				centerX, centerY, radiusX, radiusY, fade, color
		);
	}

	public void antiAliased(
			int minX, int minY, int maxX, int maxY, int centerX, int centerY,
			int radiusX, int radiusY, float fade, int color
	) {
		complex(
				minX, minY, maxX, maxY, centerX, centerY, radiusX, radiusY,
				color, color, 0, 0, 0,
				1f, 1f + fade, 100f, 100f
		);
	}
}
