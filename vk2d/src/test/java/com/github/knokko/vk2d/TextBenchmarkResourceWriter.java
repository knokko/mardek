package com.github.knokko.vk2d;

import com.github.knokko.vk2d.resource.Vk2dResourceWriter;
import org.lwjgl.system.Configuration;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

import static com.github.knokko.vk2d.text.FontHelper.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.util.freetype.FreeType.*;

public class TextBenchmarkResourceWriter {

	public static final File TEXT_RESOURCE_FILE = new File("text-benchmark-resources.bin");

	public static void main(String[] args) throws IOException {
		Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary());
		InputStream fontInput = Objects.requireNonNull(TextPlayground.class.getClassLoader().getResourceAsStream(
				"com/github/knokko/vk2d/fonts/thaana.ttf"
		));
		byte[] fontBytes = fontInput.readAllBytes();
		fontInput.close();

		var ttfBuffer = memAlloc(fontBytes.length);
		ttfBuffer.put(0, fontBytes);
		long ftLibrary;
		FT_Face font;
		try (var stack = stackPush()) {
			var pLibrary = stack.callocPointer(1);
			assertFtSuccess(FT_Init_FreeType(pLibrary), "Init_FreeType");
			ftLibrary = pLibrary.get(0);
			var pFace = stack.callocPointer(1);
			assertFtSuccess(FT_New_Memory_Face(ftLibrary, ttfBuffer, 0, pFace), "New_Memory_Face");
			font = FT_Face.create(pFace.get(0));
		}

		Vk2dResourceWriter writer = new Vk2dResourceWriter();
		writer.addFont(font);

		OutputStream output = Files.newOutputStream(TEXT_RESOURCE_FILE.toPath());
		writer.write(output);
		output.close();

		FT_Done_Face(font);
		FT_Done_FreeType(ftLibrary);
	}
}
