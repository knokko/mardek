package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.frame.Vk2dRenderStage;
import com.github.knokko.vk2d.pipeline.Vk2dPipeline;

import java.nio.ByteBuffer;

import static com.github.knokko.boiler.utilities.ColorPacker.*;

class Vk2dAbstractColorBatch extends Vk2dBatch {

	Vk2dAbstractColorBatch(Vk2dPipeline pipeline, Vk2dRenderStage frame, int initialCapacity) {
		super(pipeline, frame, initialCapacity);
	}

	public void fill(int minX, int minY, int maxX, int maxY, int color) {
		fillUnaligned(minX, maxY + 1, maxX + 1, maxY + 1, maxX + 1, minY, minX, minY, color);
	}

	public void fillUnaligned(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, int color) {
		gradientUnaligned(
				x1, y1, color,
				x2, y2, color,
				x3, y3, color,
				x4, y4, color
		);
	}

	private byte clamp(int value){
		return (byte) Math.clamp(value, 0, 255);
	}

	public void gradient(
			int minX, int minY, int maxX, int maxY,
			int colorBottomLeft, int colorBottomRight, int colorTopLeft
	) {
		// colorTopRight = colorBottomLeft + (colorBottomRight - colorBottomLeft) + (colorTopLeft - colorBottomLeft) =
		//   colorBottomRight + colorTopLeft - colorBottomLeft
		int colorTopRight = rgba(
				clamp(unsigned(red(colorBottomRight)) + unsigned(red(colorTopLeft)) - unsigned(red(colorBottomLeft))),
				clamp(unsigned(green(colorBottomRight)) + unsigned(green(colorTopLeft)) - unsigned(green(colorBottomLeft))),
				clamp(unsigned(blue(colorBottomRight)) + unsigned(blue(colorTopLeft)) - unsigned(blue(colorBottomLeft))),
				clamp(unsigned(alpha(colorBottomRight)) + unsigned(alpha(colorTopLeft)) - unsigned(alpha(colorBottomLeft)))
		);
		gradientUnaligned(
				minX, maxY + 1, colorBottomLeft,
				maxX + 1, maxY + 1, colorBottomRight,
				maxX + 1, minY, colorTopRight,
				minX, minY, colorTopLeft
		);
	}

	public void gradientUnaligned(
			int x1, int y1, int color1,
			int x2, int y2, int color2,
			int x3, int y3, int color3,
			int x4, int y4, int color4
	) {
		ByteBuffer vertices = putTriangles(2).vertexData()[0];
		putCompressedPosition(vertices, x1, y1);
		vertices.putInt(color1);
		putCompressedPosition(vertices, x2, y2);
		vertices.putInt(color2);
		putCompressedPosition(vertices, x3, y3);
		vertices.putInt(color3);

		putCompressedPosition(vertices, x3, y3);
		vertices.putInt(color3);
		putCompressedPosition(vertices, x4, y4);
		vertices.putInt(color4);
		putCompressedPosition(vertices, x1, y1);
		vertices.putInt(color1);
	}
}
