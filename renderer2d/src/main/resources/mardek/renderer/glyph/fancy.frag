#version 450

#include "info.glsl"

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in float relativeY;
layout(location = 2) in flat GlyphInfo glyph;

layout(location = 0) out vec4 outColor;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/glyph/intersection.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/glyph/color.glsl"

void main() {
	WaveIntersection intersection = closestIntersection(glyph);
	float intensity = 0.0;
	float middle = 0.4;
	if (relativeY >= middle) intensity = 1.0 - (relativeY - middle) / (1.0 - middle);
	else intensity = relativeY / middle;
	intensity *= intensity * intensity;

	vec4 fillColor = intensity * vec4(0.9, 0.75, 0.7, 1.0) + (1.0 - intensity) * vec4(0.42, 0.21, 0.02, 1.0);
	vec4 mainColor = determineMainColor(
		intersection.inside, intersection.distance, fillColor, decodeColor(glyph.backgroundColor)
	);
	float strokeIntensity = determineStrokeIntensity(intersection.distance, glyph.strokeWidth);
	if (intersection.inside) strokeIntensity = 0.0;
	outColor = mixStrokeColor(mainColor, decodeColor(glyph.strokeColor), strokeIntensity);
}
