#include "../decode.glsl"

struct TextStyle {
	uvec4 rawIntegers;
	vec4 rawFloats;
};

vec4 getFillColor(TextStyle style) {
	return decodeColor(style.rawIntegers[0]);
}

vec4 getStrokeColor(TextStyle style) {
	return decodeColor(style.rawIntegers[1]);
}

float getDistanceFactor(TextStyle style) {
	return style.rawFloats[0];
}

float getDistanceBias(TextStyle style) {
	return style.rawFloats[1];
}

float getStrokeWidth(TextStyle style) {
	return style.rawFloats[2];
}

float getStrokePower(TextStyle style) {
	return style.rawFloats[3];
}

bool isStrokeBehindFillColor(TextStyle style) {
	return style.rawIntegers[2] == 1;
}
