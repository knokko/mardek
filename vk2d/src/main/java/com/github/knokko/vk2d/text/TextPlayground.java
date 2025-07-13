package com.github.knokko.vk2d.text;

import java.io.IOException;
import java.util.Objects;

public class TextPlayground {

	public static void main(String[] args) throws IOException {
		var fontInput = Objects.requireNonNull(TextPlayground.class.getClassLoader().getResourceAsStream(
				"com/github/knokko/ui/renderer/Code2003.ttf"
		));
		var fontBytes = fontInput.readAllBytes();
		fontInput.close();

		var manager = new FontManager();
		var font = manager.addFont(fontBytes);
	}
}
