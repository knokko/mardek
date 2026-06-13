package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.buffers.VkbBuffer;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.vk2d.Vk2dInstance;
import org.lwjgl.util.harfbuzz.hb_glyph_extents_t;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.boiler.utilities.BoilerMath.leastCommonMultiple;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_draw_funcs_destroy;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_draw_funcs_set_cubic_to_func;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_draw_funcs_set_quadratic_to_func;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_font_draw_glyph_or_fail;
import static org.lwjgl.vulkan.VK10.*;

class SdfOutlines {

	private final int[] info;
	private List<Line> lines = new ArrayList<>();
	private List<Curve> curves = new ArrayList<>();

	VkbBuffer persistentBuffer;

	SdfOutlines(
			long hbFont, hb_glyph_extents_t.Buffer glyphExtents, Vk2dInstance instance,
			MemoryCombiner persistentMemory
	) {
		this.info = new int[2 * (glyphExtents.capacity() + 1)];
		var drawFunctions = hb_draw_funcs_create();

		int[] currentCoordinates = new int[4];

		hb_draw_funcs_set_move_to_func(drawFunctions, (dfuncs, draw_data, st, to_x, to_y, user_data) -> {
			currentCoordinates[0] = (int) to_x;
			currentCoordinates[1] = (int) to_y;
		}, 0L, null);
		hb_draw_funcs_set_line_to_func(drawFunctions, (dfuncs, draw_data, st, to_x, to_y, user_data) -> {
			int nextX = (int) to_x;
			int nextY = (int) to_y;
			if (nextX == currentCoordinates[0] && nextY == currentCoordinates[1]) return;
			lines.add(new Line(
					currentCoordinates[0] - currentCoordinates[2],
					currentCoordinates[1] - currentCoordinates[3],
					nextX - currentCoordinates[2],
					nextY - currentCoordinates[3]
			));
			currentCoordinates[0] = nextX;
			currentCoordinates[1] = nextY;
		}, 0L, null);
		hb_draw_funcs_set_quadratic_to_func(drawFunctions, (dfuncs, draw_data, st, control_x, control_y, to_x, to_y, user_data) -> {
			if (to_x == currentCoordinates[0] && to_y == currentCoordinates[1]) return;

			float dx1 = control_x - currentCoordinates[0];
			float dy1 = control_y - currentCoordinates[1];
			float length1 = (float) sqrt(dx1 * dx1 + dy1 * dy1);
			dx1 /= length1;
			dy1 /= length1;

			float dx2 = to_x - currentCoordinates[0];
			float dy2 = to_y - currentCoordinates[1];
			float length2 = (float) sqrt(dx2 * dx2 + dy2 * dy2);
			dx2 /= length2;
			dy2 /= length2;

			// When the inner product is too close to 1, the curve is basically a line segment.
			// Replacing it with a real line segment improves performance and avoids precision issues and divisions by 0
			float innerProduct = dx1 * dx2 + dy1 * dy2;
			if (innerProduct < 1.99) {
				curves.add(new Curve(
						currentCoordinates[0] - currentCoordinates[2],
						currentCoordinates[1] - currentCoordinates[3],
						(int) control_x - currentCoordinates[2],
						(int) control_y - currentCoordinates[3],
						(int) to_x - currentCoordinates[2],
						(int) to_y - currentCoordinates[3]
				));
			} else {
				lines.add(new Line(
						currentCoordinates[0] - currentCoordinates[2],
						currentCoordinates[1] - currentCoordinates[3],
						(int) to_x - currentCoordinates[2],
						(int) to_y - currentCoordinates[3]
				));
			}

			currentCoordinates[0] = (int) to_x;
			currentCoordinates[1] = (int) to_y;
		}, 0L, null);
		hb_draw_funcs_set_cubic_to_func(drawFunctions, (dfuncs, draw_data, st, control1_x, control1_y, control2_x, control2_y, to_x, to_y, user_data) -> {
			System.out.println("Warning: cubic sdf curves are not supported");
			currentCoordinates[0] = (int) to_x;
			currentCoordinates[1] = (int) to_y;
		}, 0L, null);

		for (int glyph = 0; glyph < glyphExtents.capacity(); glyph++) {
			var extents = glyphExtents.get(glyph);
			currentCoordinates[2] = min(extents.x_bearing(), extents.x_bearing() + extents.width());
			currentCoordinates[3] = min(extents.y_bearing(), extents.y_bearing() + extents.height());
			this.info[2 * glyph] = lines.size();
			this.info[2 * glyph + 1] = curves.size();
			hb_font_draw_glyph_or_fail(hbFont, glyph, drawFunctions, 0L);
		}

		this.info[2 * glyphExtents.capacity()] = lines.size();
		this.info[2 * glyphExtents.capacity() + 1] = curves.size();

		hb_draw_funcs_destroy(drawFunctions);

		this.persistentBuffer = persistentMemory.addBuffer(
				determineStagingBufferSize(),
				leastCommonMultiple(4L, instance.boiler.deviceProperties.limits().minStorageBufferOffsetAlignment()),
				VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
				0.5f
		);
	}

	int determineStagingBufferSize() {
		return 8 * lines.size() + 12 * curves.size();
	}

	void fillBuffer(ByteBuffer destination) {
		var data = destination.asIntBuffer();

		for (var line : lines) {
			data.put(line.startX | (line.startY << 16));
			data.put(line.endX | (line.endY << 16));
		}

		for (var curve : curves) {
			data.put(curve.startX | (curve.startY << 16));
			data.put(curve.controlX | (curve.controlY << 16));
			data.put(curve.endX | (curve.endY << 16));
		}

		lines = null;
		curves = null;
	}

	private record Line(int startX, int startY, int endX, int endY) {}

	private record Curve(int startX, int startY, int controlX, int controlY, int endX, int endY) {}

	int getLinesOffset(int glyph) {
		return 2 * info[2 * glyph];
	}

	int getNumLines(int glyph) {
		return info[2 * glyph + 2] - info[2 * glyph];
	}

	int getCurvesOffset(int glyph) {
		return 2 * info[info.length - 2] + 3 * info[2 * glyph + 1];
	}

	int getNumCurves(int glyph) {
		return info[2 * glyph + 3] - info[2 * glyph + 1];
	}
}
