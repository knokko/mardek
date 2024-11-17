package com.github.knokko.ui.renderer;

import com.github.knokko.text.bitmap.GlyphRasterizer;
import com.github.knokko.text.font.FontData;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_GlyphSlot;

import java.nio.ByteBuffer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static java.lang.Byte.toUnsignedInt;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.freetype.FreeType.FT_LOAD_RENDER;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;

class OutlineGlyphRasterizer implements GlyphRasterizer {

	private final FontData font;
	private int width, height;
	private ByteBuffer buffer;

	public int outlineWidth;

	OutlineGlyphRasterizer(FontData font) {
		this.font = font;
	}

	private int get(int x, int y) {
		if (x < 0 || x >= width) return 0;
		if (y < 0 || y >= height) return 0;
		return get(x + y * width);
	}

	private int get(int index) {
		return toUnsignedInt(buffer.get(index));
	}

	@Override
	public void set(int glyph, int faceIndex, int size) {
		String context = "FreeTypeGlyphRasterizer.set(" + glyph + ", " + faceIndex + ", " + size + ")";
		var face = font.borrowFaceWithSize(faceIndex, size, 1);
		assertFtSuccess(FT_Load_Glyph(face.ftFace, glyph, FT_LOAD_RENDER), "Load_Glyph", context);

		FT_GlyphSlot slot = face.ftFace.glyph();
		if (slot == null) throw new Error("Glyph slot must not be null at this point");

		@SuppressWarnings("resource") FT_Bitmap bitmap = slot.bitmap();
		this.width = bitmap.width() + 2 * outlineWidth;
		this.height = bitmap.rows() + 2 * outlineWidth;

		if (this.buffer == null || this.buffer.capacity() < this.width * this.height) {
			if (this.buffer != null) memFree(this.buffer);
			this.buffer = memCalloc(2 * this.width * this.height);
		}

		this.buffer.position(0);
		this.buffer.limit(this.width * this.height);
		for (int index = 0; index < this.width * this.height; index++) this.buffer.put(index, (byte) 0);
		if (bitmap.width() > 0 && bitmap.rows() > 0) {
			var source = bitmap.buffer(bitmap.width() * bitmap.rows());
			for (int y = 0; y < bitmap.rows(); y++) {
				this.buffer.put((outlineWidth + y) * this.width + outlineWidth, source, y * bitmap.width(), bitmap.width());
			}

			for (int index = 0; index < buffer.limit(); index++) {
				if (get(index) > 255 - outlineWidth) buffer.put(index, (byte) (255 - outlineWidth));
			}

			for (int o = 0; o < outlineWidth; o++) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int currentIntensity = get(x, y);
						if (currentIntensity >= 255 - outlineWidth) continue;

						int[] neighbours = { get(x - 1, y), get(x + 1, y), get(x, y - 1), get(x, y + 1) };
						for (int n : neighbours) {
							if ((o == 0 && n == 255 - outlineWidth) || (o != 0 && n == 256 - o)) {
								buffer.put(x + y * width, (byte) (255 - o));
							}
						}
					}
				}
			}
		}

		font.returnFace(face);
	}

	@Override
	public int getBufferWidth() {
		return width;
	}

	@Override
	public int getBufferHeight() {
		return height;
	}

	@Override
	public ByteBuffer getBuffer() {
		return buffer;
	}

	@Override
	public void destroy() {
		if (buffer != null) memFree(buffer);
	}
}
