package com.github.knokko.vk2d.text;

import static com.github.knokko.boiler.utilities.ColorPacker.multiplyColors;
import static java.lang.Math.max;

public record Vk2dFancyTextStyle(
		Gradient fillColor, float fillDistanceFactor, float fillDistanceBias,
		Gradient innerStrokeColor, Gradient outerStrokeColor, boolean strokeBehindFill
) {

	public static Vk2dFancyTextStyle withoutStroke(
			Gradient fillColor, float fillDistanceFactor, float fillDistanceBias
	) {
		return new Vk2dFancyTextStyle(
				fillColor, fillDistanceFactor, fillDistanceBias,
				Gradient.plain(0), Gradient.plain(0), true
		);
	}

	/**
	 * Creates and returns a new <i>Vk2dFancyTextStyle</i> that is a copy of this style,
	 * except that all colors are multiplied with {@code multiplyColor},
	 * using {@link com.github.knokko.boiler.utilities.ColorPacker#multiplyColors}.
	 */
	public Vk2dFancyTextStyle multiply(int multiplyColor) {
		return new Vk2dFancyTextStyle(
				fillColor.multiply(multiplyColor), fillDistanceFactor, fillDistanceBias,
				innerStrokeColor.multiply(multiplyColor), outerStrokeColor.multiply(multiplyColor), strokeBehindFill
		);
	}

	public float getEffectiveStrokeWidth() {
		float strokeWidth = 0;
		float t = 100f;
		if (outerStrokeColor.threshold0 < t) strokeWidth = max(strokeWidth, outerStrokeColor.threshold0);
		if (outerStrokeColor.threshold1 < t) strokeWidth = max(strokeWidth, outerStrokeColor.threshold1);
		if (outerStrokeColor.threshold2 < t) strokeWidth = max(strokeWidth, outerStrokeColor.threshold2);
		if (outerStrokeColor.threshold3 < t) strokeWidth = max(strokeWidth, outerStrokeColor.threshold3);
		return strokeWidth;
	}

	public record Gradient(
			int baseColor, int color0, int color1, int color2, int color3,
			float threshold0, float threshold1, float threshold2, float threshold3
	) {
		public static Gradient plain(int color) {
			float t = 1234567.8f;
			return new Gradient(color, color, 0, 0, 0, t, t, t, t);
		}

		/**
		 * Creates and returns a <i>Gradient</i> that is a copy of this gradient,
		 * except that all its colors are multiplied with {@code multiplyColor},
		 * using {@link com.github.knokko.boiler.utilities.ColorPacker#multiplyColors}.
		 */
		public Gradient multiply(int multiplyColor) {
			return new Gradient(
					multiplyColors(baseColor, multiplyColor),
					multiplyColors(color0, multiplyColor), multiplyColors(color1, multiplyColor),
					multiplyColors(color2, multiplyColor), multiplyColors(color3, multiplyColor),
					threshold0, threshold1, threshold2, threshold3
			);
		}
	}

	public record Shadowed(Vk2dFancyTextStyle mainStyle, Vk2dFancyTextStyle shadowStyle, float shadowOffset) {}
}
