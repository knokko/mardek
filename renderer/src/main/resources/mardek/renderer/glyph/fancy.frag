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

	WaveIntersection intersection = closestIntersection(glyph, glyph.yInfoAndStrokeWidth.x > 1.0);

	if (glyph.yInfoAndStrokeWidth.x > 0.0) {
		if (intersection.inside) {
			float strokeIntensity = determineStrokeIntensity(intersection.distance, 0.75);
			vec4 strokeColor = decodeColor(glyph.colorsAndSize.w);
			outColor = mixStrokeColor(fillColor, strokeColor, strokeIntensity);
		} else {
			float strokeIntensity = determineStrokeIntensity(intersection.distance, glyph.yInfoAndStrokeWidth.x);
			vec4 strokeColor = computeGradientColor(intersection.distance, glyph.colorsAndSize.w, glyph.borderDistances, glyph.borderColors);
			outColor = mixStrokeColor(vec4(0.0), strokeColor, strokeIntensity);
		}
	} else {
		outColor = determineMainColor(intersection.inside, intersection.distance, fillColor);
	}
}
