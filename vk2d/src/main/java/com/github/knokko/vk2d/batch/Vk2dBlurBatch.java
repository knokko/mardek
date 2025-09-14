package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.nio.ByteBuffer;

/**
 * This is the batch class of {@link com.github.knokko.vk2d.pipeline.Vk2dBlurPipeline}. See the blur pipeline docs
 * (link is in the README) for more information.
 */
public class Vk2dBlurBatch extends Vk2dBatch {

	public final int textureWidth, textureHeight;
	public final float minX, minY, boundX, boundY;
	public final long descriptorSet;

	/**
	 * This method is for internal use only. Use {@link com.github.knokko.vk2d.pipeline.Vk2dBlurPipeline#addBatch}
	 */
	public Vk2dBlurBatch(
			Vk2dPipeline pipeline, Vk2dRenderStage frame,
			int textureWidth, int textureHeight,
			float minX, float minY, float boundX, float boundY,
			long descriptorSet
	) {
		super(pipeline, frame, 2);
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.minX = normalizeX(minX);
		this.minY = normalizeY(minY);
		this.boundX = normalizeX(boundX);
		this.boundY = normalizeY(boundY);
		this.descriptorSet = descriptorSet;
	}

	public void gradientColorTransform(
			int addColor1, int multiplyColor1,
			int addColor2, int multiplyColor2,
			int addColor3, int multiplyColor3,
			int addColor4, int multiplyColor4
	) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];
		vertices.putInt(addColor1).putInt(multiplyColor1);
		vertices.putInt(addColor2).putInt(multiplyColor2);
		vertices.putInt(addColor3).putInt(multiplyColor3);

		vertices.putInt(addColor3).putInt(multiplyColor3);
		vertices.putInt(addColor4).putInt(multiplyColor4);
		vertices.putInt(addColor1).putInt(multiplyColor1);
	}

	public void fixedColorTransform(int addColor, int multiplyColor) {
		gradientColorTransform(
				addColor, multiplyColor, addColor, multiplyColor,
				addColor, multiplyColor, addColor, multiplyColor
		);
	}

	public void noColorTransform() {
		fixedColorTransform(0, -1);
	}
}
