#version 450

#include "info.glsl"

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in float relativeY;
layout(location = 2) in flat GlyphInfo glyph;

layout(location = 0) out vec4 outColor;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/linear-gradients.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/glyph/intersection.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/glyph/color.glsl"

void main() {
	vec4 fillColor = computeGradientColor(relativeY, glyph.colorsAndSize.z, glyph.fillDistances, glyph.fillColors);

	WaveIntersection intersection = closestIntersection(glyph);
	vec4 mainColor = determineMainColor(intersection.inside, intersection.distance, fillColor);
	float strokeIntensity = determineStrokeIntensity(intersection.distance, glyph.yInfoAndStrokeWidth.x);
	if (intersection.inside) strokeIntensity = 0.0;

	vec4 strokeColor;
	if (strokeIntensity > 0.0) {
		strokeColor = computeGradientColor(intersection.distance, glyph.colorsAndSize.w, glyph.borderDistances, glyph.borderColors);
	} else strokeColor = decodeColor(glyph.colorsAndSize.w);

	outColor = mixStrokeColor(mainColor, strokeColor, strokeIntensity);
}
