package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dOvalPipeline;

import java.nio.ByteBuffer;

import static com.github.knokko.vk2d.pipeline.Vk2dOvalPipeline.OVAL_SIZE;

/**
 * This is the batch class of {@link com.github.knokko.vk2d.pipeline.Vk2dOvalPipeline}. See the oval pipeline docs
 * (link is in the README) for more information.
 */
public class Vk2dOvalBatch extends Vk2dBatch {

	public final long perFrameDescriptorSet;

	/**
	 * This method is for internal use only. Use {@link com.github.knokko.vk2d.pipeline.Vk2dOvalPipeline#addBatch}
	 */
	public Vk2dOvalBatch(Vk2dOvalPipeline pipeline, Vk2dRenderStage frame, long perFrameDescriptorSet, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
		this.perFrameDescriptorSet = perFrameDescriptorSet;
	}

	public void complex(
			int minX, int minY, int maxX, int maxY,
			float centerX, float centerY, float radiusX, float radiusY,
			int centerColor, int color0, int color1, int color2, int color3,
			float distance0, float distance1, float distance2, float distance3
	) {
		MiniBatch triangles = putTriangles(2);

		ByteBuffer oval = triangles.vertexData()[1];
		int ovalByteOffset = Math.toIntExact(oval.position() + triangles.vertexBuffers()[1].offset - perFrameBuffer.buffer.offset);
		int ovalIndex = ovalByteOffset / OVAL_SIZE;

		oval.putInt(color0).putInt(color1).putInt(color2).putInt(color3);
		oval.putFloat(distance0 * distance0).putFloat(distance1 * distance1);
		oval.putFloat(distance2 * distance2).putFloat(distance3 * distance3);
		oval.putFloat(normalizeX(centerX)).putFloat(normalizeY(centerY));
		oval.putFloat(2f * radiusX / width).putFloat(2f * radiusY / height);
		oval.putInt(centerColor).putInt(0).putInt(0).putInt(0);

		ByteBuffer vertices = triangles.vertexData()[0];
		putCompressedPosition(vertices, minX, maxY + 1);
		vertices.putInt(ovalIndex);
		putCompressedPosition(vertices, maxX + 1, maxY + 1);
		vertices.putInt(ovalIndex);
		putCompressedPosition(vertices, maxX + 1, minY);
		vertices.putInt(ovalIndex);

		putCompressedPosition(vertices, maxX + 1, minY);
		vertices.putInt(ovalIndex);
		putCompressedPosition(vertices, minX, minY);
		vertices.putInt(ovalIndex);
		putCompressedPosition(vertices, minX, maxY + 1);
		vertices.putInt(ovalIndex);
	}

	public void simpleAliased(int minX, int minY, int maxX, int maxY, int color) {
		int centerX = (minX + maxX) / 2;
		int centerY = (minY + maxY) / 2;
		aliased(minX, minY, maxX, maxY, centerX, centerY, centerX - minX, centerY - minY, color);
	}

	public void aliased(
			int minX, int minY, int maxX, int maxY,
			float centerX, float centerY, float radiusX, float radiusY, int color
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
			int minX, int minY, int maxX, int maxY, float centerX, float centerY,
			float radiusX, float radiusY, float fade, int color
	) {
		complex(
				minX, minY, maxX, maxY, centerX, centerY, radiusX, radiusY,
				color, color, 0, 0, 0,
				1f, 1f + fade, 100f, 100f
		);
	}
}
