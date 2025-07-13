package com.github.knokko.vk2d.text;

import org.lwjgl.system.Configuration;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import static com.github.knokko.vk2d.text.FontHelper.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.util.freetype.FreeType.FT_Init_FreeType;
import static org.lwjgl.util.freetype.FreeType.FT_New_Memory_Face;

public class FontManager {

	static {
		Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary());
	}

	private final long ftLibrary;

	public FontManager() {
		try (var stack = stackPush()) {
			var pLibrary = stack.callocPointer(1);
			assertFtSuccess(FT_Init_FreeType(pLibrary), "Init_FreeType");
			this.ftLibrary = pLibrary.get(0);
		}
	}

	public Font addFont(byte[] ttfBytes) {
		var ttfBuffer = memAlloc(ttfBytes.length);
		ttfBuffer.put(0, ttfBytes);
		try (var stack = stackPush()) {
			var pFace = stack.callocPointer(1);
			assertFtSuccess(FT_New_Memory_Face(ftLibrary, ttfBuffer, 0, pFace), "New_Memory_Face");
			return new Font(FT_Face.create(pFace.get(0)));
		}
	}
}
