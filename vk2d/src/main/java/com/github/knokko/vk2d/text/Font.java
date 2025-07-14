package com.github.knokko.vk2d.text;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import org.lwjgl.system.Configuration;
import org.lwjgl.util.freetype.*;
import org.lwjgl.util.harfbuzz.HarfBuzz;

import java.nio.ByteBuffer;
import java.util.*;

import static com.github.knokko.vk2d.text.FontHelper.assertFtSuccess;
import static java.lang.Math.clamp;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

public class Font {

	private final FT_Face ftFace;
	private final long hbBuffer = hb_buffer_create();

	private final Map<String, ShapedString> shapeMap = new HashMap<>();

	Font(FT_Face ftFace) {
		this.ftFace = ftFace;

		try (var stack = stackPush()) {
//			var buffer = hb_buffer_create();
//			hb_buffer_add_utf8(buffer, stack.UTF8("MARDEK the w'eird"), 0, -1);
//			hb_buffer_guess_segment_properties(buffer);
//			var blob = hb_blob_create(this.ttfData, HB_MEMORY_MODE_READONLY, 0L, null);
//			var face = hb_face_create(blob, 0);
//			var font = hb_font_create(face);
//			hb_shape(font, buffer, null);
//			var glyphInfo = Objects.requireNonNull(hb_buffer_get_glyph_infos(buffer));
//			var glyphPositions = Objects.requireNonNull(hb_buffer_get_glyph_positions(buffer));
//			System.out.println("no crash... yet");
//			for (int index = 0; index < glyphInfo.capacity(); index++) {
//				var info = glyphInfo.get(index);
//				var position = glyphPositions.get(index);
//				System.out.println("glyph index is " + info.codepoint() + " and position is (" + position.x_offset() + ", " + position.y_offset() + ") and advance " + position.x_advance());
//			}

//			assertFtSuccess(FT_Load_Glyph(ftFace, glyphInfo.get(3).codepoint(), FT_LOAD_NO_SCALE), "Load_Glyph");
			int glyphA = FT_Get_Char_Index(ftFace, 'A');
			System.out.println("A is " + glyphA);
			var outline = Objects.requireNonNull(ftFace.glyph()).outline();
			System.out.println("#points is " + outline.n_points() + " and #contours is " + outline.n_contours());
			for (int index = 0; index < outline.n_points(); index++) {
				var point = outline.points().get(index);
				System.out.println("point is (" + point.x() + ", " + point.y() + ") and tag is " + outline.tags().get(index));
			}
			for (int index = 0; index < outline.n_contours(); index++) {
				System.out.println("contour is " + outline.contours().get(index));
			}

			var position = FT_Vector.calloc(stack);
			var outlineFunctions = FT_Outline_Funcs.calloc(stack);

			int[] counter = { 0, 0 };
			List<Line> lines = new ArrayList<>();
			List<Curve> curves = new ArrayList<>();
			outlineFunctions.move_to((long raw, long userData) -> {
				@SuppressWarnings("resource") var to = FT_Vector.create(raw);
				position.x(to.x());
				position.y(to.y());
				return 0;
			});
			outlineFunctions.line_to((long raw, long userData) -> {
				@SuppressWarnings("resource") var to = FT_Vector.create(raw);
				lines.add(new Line(position.x(), position.y(), to.x(), to.y()));
				position.x(to.x());
				position.y(to.y());
				counter[0] += 1;
				return 0;
			});
			outlineFunctions.conic_to((long rawControl, long rawTo, long userData) -> {
				@SuppressWarnings("resource") var control = FT_Vector.create(rawControl);
				@SuppressWarnings("resource") var to = FT_Vector.create(rawTo);
				curves.add(new Curve(position.x(), position.y(), control.x(), control.y(), to.x(), to.y()));
				position.x(to.x());
				position.y(to.y());
				counter[1] += 1;
				return 0;
			});
			outlineFunctions.cubic_to((long control1, long control2, long to, long userData) -> 1);
			outlineFunctions.delta(0);
			outlineFunctions.shift(0);

			assertFtSuccess(FT_Load_Glyph(ftFace, glyphA, FT_LOAD_NO_SCALE), "Load_Glyph");
			assertFtSuccess(FT_Outline_Decompose(outline, outlineFunctions, 0L), "Outline_Decompose");

			System.out.println("lines are " + lines);
			System.out.println("curves are " + curves);

			for (int y = 0; y < 50; y++) {
				for (int x = 0; x < 50; x++) {
					int windingNumber = 0;
					float alpha = 0;
					for (Line line : lines) {
						float a = 0f;
						float b = 0f;
						float dx0 = line.startX / 20f - x;
						float dy0 = line.startY / 20f - y;
						float dx2 = line.endX / 20f - x;
						float dy2 = line.endY / 20f - y;
						float c = dx0;

						float t = dy0 / (dy0 - dy2);
						float t0, t1;
						if (dy0 < dy2) {
							t0 = -1.0f;
							t1 = t;
						} else {
							t0 = t;
							t1 = -1.0f;
						}

						float inverseDiameter = 1f;

						if (t0 >= 0 && t0 < 1) {
							float r = (a*t0 - 2.0f*b)*t0 + c;
							alpha += clamp(r * inverseDiameter + 0.5, 0, 1);
						}

						if (t1 >= 0 && t1 < 1) {
							float r = (a*t1 - 2.0f*b)*t1 + c;
							alpha -= clamp(r * inverseDiameter + 0.5, 0, 1);
						}



					}
					System.out.print(alpha > 0.5f ? 'X' : ' ');
					System.out.print(' ');
				}

				System.out.println();
			}

//			long startTime = System.nanoTime();
//			for (int glyph = 0; glyph < ftFace.num_glyphs(); glyph++) {
//				System.out.println("Glyph is " + glyph + " and #glyphs is " + ftFace.num_glyphs());
//				assertFtSuccess(FT_Load_Glyph(ftFace, glyph, FT_LOAD_NO_SCALE), "Load_Glyph");
//				outline = Objects.requireNonNull(ftFace.glyph()).outline();
//				assertFtSuccess(FT_Outline_Decompose(outline, outlineFunctions, 0L), "Outline_Decompose");
//			}
//			System.out.println("Took " + (System.nanoTime() - startTime) / 1000 + "us");
//			System.out.println(Arrays.toString(counter));
//			hb_buffer_destroy(buffer);
//			hb_font_destroy(font);
//			hb_face_destroy(face);
//			hb_blob_destroy(blob);
		}
	}

	void destroy() {
		assertFtSuccess(FT_Done_Face(ftFace), "Done_Face");
	}

	record Line(long startX, long startY, long endX, long endY) {}

	record Curve(long startX, long startY, long controlX, long controlY, long endX, long endY) {}
}
