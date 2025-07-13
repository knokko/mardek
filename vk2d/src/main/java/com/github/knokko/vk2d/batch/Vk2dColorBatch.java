package com.github.knokko.vk2d.batch;

import com.github.knokko.vk2d.Vk2dFrame;
import com.github.knokko.vk2d.pipeline.Vk2dColorPipeline;

import java.nio.ByteBuffer;

import static com.github.knokko.boiler.utilities.ColorPacker.*;
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
		ByteBuffer vertices = putVertices(6);
		putPosition(vertices, x1, y1);
		vertices.putInt(color1);
		putPosition(vertices, x2, y2);
		vertices.putInt(color2);
		putPosition(vertices, x3, y3);
		vertices.putInt(color3);

		putPosition(vertices, x3, y3);
		vertices.putInt(color3);
		putPosition(vertices, x4, y4);
		vertices.putInt(color4);
		putPosition(vertices, x1, y1);
		vertices.putInt(color1);
	}
}
