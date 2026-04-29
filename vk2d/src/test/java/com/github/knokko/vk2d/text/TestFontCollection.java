package com.github.knokko.vk2d.text;

import java.io.InputStream;

public class TestFontCollection {

	public static InputStream myriadFont() {
		return TestFontCollection.class.getClassLoader().getResourceAsStream(
				"com/github/knokko/vk2d/fonts/myriad-condensed-web.ttf"
		);
	}

	public static InputStream thaanaFont() {
		return TestFontCollection.class.getClassLoader().getResourceAsStream(
				"com/github/knokko/vk2d/fonts/thaana.ttf"
		);
	}
}
