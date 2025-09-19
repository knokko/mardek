package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;
import com.github.knokko.vk2d.resource.Vk2dResourceBundle;

import java.nio.ByteBuffer;

import static java.lang.Math.*;

/**
 * This is the batch class of {@link com.github.knokko.vk2d.pipeline.Vk2dKimPipeline}. See the kim pipeline docs
 * (link is in the README) for more information.
 */
public class Vk2dKimBatch extends Vk2dBatch {

	public final Vk2dResourceBundle bundle;

	public Vk2dKimBatch(Vk2dPipeline pipeline, Vk2dRenderStage stage, int initialCapacity, Vk2dResourceBundle bundle) {
		super(pipeline, stage, initialCapacity);
		this.bundle = bundle;
	}

	/**
	 * This method is for internal use only. Use {@link com.github.knokko.vk2d.pipeline.Vk2dKimPipeline#addBatch}
	 */
	public void simple(int minX, int minY, int maxX, int maxY, int textureIndex) {
		int textureOffset = bundle.getFakeImageOffset(textureIndex);
		ByteBuffer vertices = putTriangles(2).vertexData()[0];

		putCompressedPosition(vertices, minX, maxY + 1);
		vertices.putFloat(0f).putFloat(1f).putInt(textureOffset);
		putCompressedPosition(vertices, maxX + 1, maxY + 1);
		vertices.putFloat(1f).putFloat(1f).putInt(textureOffset);
		putCompressedPosition(vertices, maxX + 1, minY);
		vertices.putFloat(1f).putFloat(0f).putInt(textureOffset);

		putCompressedPosition(vertices, maxX + 1, minY);
		vertices.putFloat(1f).putFloat(0f).putInt(textureOffset);
		putCompressedPosition(vertices, minX, minY);
		vertices.putFloat(0f).putFloat(0f).putInt(textureOffset);
		putCompressedPosition(vertices, minX, maxY + 1);
		vertices.putFloat(0f).putFloat(1f).putInt(textureOffset);
	}

	public void simple(int minX, int minY, int scale, int textureIndex) {
		simple(minX, minY, (float) scale, textureIndex);
	}

	public void simple(int minX, int minY, float scale, int textureIndex) {
		int width = Math.round(scale * bundle.getFakeImageWidth(textureIndex));
		int height = Math.round(scale * bundle.getFakeImageHeight(textureIndex));
		simple(minX, minY, minX + width - 1, minY + height - 1, textureIndex);
	}

	public void rotated(float midX, float midY, float angle, float scale, int textureIndex) {
		float hw = 0.5f * bundle.getFakeImageWidth(textureIndex) * scale;
		float hh = 0.5f * bundle.getFakeImageHeight(textureIndex) * scale;
		float rawAngle = (float) toRadians(angle);
		float sa = (float) sin(rawAngle);
		float ca = (float) cos(rawAngle);
		transformed(
				round(midX - hw * ca + hh * sa), round(midY + hh * ca + hw * sa),
				round(midX + hw * ca + hh * sa), round(midY + hh * ca - hw * sa),
				round(midX + hw * ca - hh * sa), round(midY - hh * ca - hw * sa),
				round(midX - hw * ca - hh * sa), round(midY - hh * ca + hw * sa),
				textureIndex
		);
	}

	public void transformed(
			int x1, int y1, int x2, int y2,
			int x3, int y3, int x4, int y4,
			int textureIndex
	) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];
		int textureOffset = bundle.getFakeImageOffset(textureIndex);

		putCompressedPosition(vertices, x1, y1);
		vertices.putFloat(0f).putFloat(1f).putInt(textureOffset);
		putCompressedPosition(vertices, x2, y2);
		vertices.putFloat(1f).putFloat(1f).putInt(textureOffset);
		putCompressedPosition(vertices, x3, y3);
		vertices.putFloat(1f).putFloat(0f).putInt(textureOffset);

		putCompressedPosition(vertices, x3, y3);
		vertices.putFloat(1f).putFloat(0f).putInt(textureOffset);
		putCompressedPosition(vertices, x4, y4);
		vertices.putFloat(0f).putFloat(0f).putInt(textureOffset);
		putCompressedPosition(vertices, x1, y1);
		vertices.putFloat(0f).putFloat(1f).putInt(textureOffset);
	}
}
