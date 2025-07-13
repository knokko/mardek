package com.github.knokko.vk2d.text;

import org.lwjgl.util.freetype.FreeType;

import static com.github.knokko.boiler.utilities.ReflectionHelper.getIntConstantName;
import static org.lwjgl.util.freetype.FreeType.FT_Err_Ok;

public class FontHelper {

	public static void assertFtSuccess(int result, String functionName) {
		if (result != FT_Err_Ok) {
			String description = getIntConstantName(FreeType.class, result, "FT_Err", "", "unknown");
			throw new RuntimeException("FT_" + functionName + " returned " + result + " (" + description + ")");
		}
	}
}
