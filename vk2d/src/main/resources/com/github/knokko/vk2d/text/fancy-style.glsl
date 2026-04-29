#include "../decode.glsl"
#include "../linear-gradients.glsl"

struct FancyTextStyle {
	uvec4 fillColors;
	vec4 fillThresholds;
	uvec4 innerStrokeColors;
	vec4 innerStrokeThresholds;
	uvec4 outerStrokeColors;
	vec4 outerStrokeThresholds;
	uvec4 looseIntegers;
	vec4 looseFloats;
};

vec4 getFillColor(FancyTextStyle style, float y) {
	return computeGradientColor(y, style.looseIntegers[0], style.fillThresholds, style.fillColors);
}

vec4 getStrokeColor(FancyTextStyle style, float signedDistance) {
	if (signedDistance >= 0.0) {
		return computeGradientColor(
			signedDistance, style.looseIntegers[1],
			style.innerStrokeThresholds, style.innerStrokeColors
		);
	} else {
		return computeGradientColor(
			-signedDistance, style.looseIntegers[2],
			style.outerStrokeThresholds, style.outerStrokeColors
		);
	}
}

bool shouldStrokeBehindFill(FancyTextStyle style) {
	return style.looseIntegers[3] == 1;
}

float getDistanceFactor(FancyTextStyle style) {
	return style.looseFloats[0];
}

float getDistanceBias(FancyTextStyle style) {
	return style.looseFloats[1];
}
