package com.github.knokko.vk2d.text;

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
	}

	public record Shadowed(Vk2dFancyTextStyle mainStyle, Vk2dFancyTextStyle shadowStyle, float shadowOffset) {}
}
