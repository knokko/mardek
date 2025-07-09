#version 450

#include "info.glsl"

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat GlyphInfo glyph;

layout(location = 0) out vec4 outColor;

#include "../decode.glsl"
#include "intersection.glsl"
#include "color.glsl"

void main() {
	WaveIntersection intersection = closestIntersection(glyph, glyph.strokeWidth > 1.0 && glyph.colorsAndSize.w != 0);
	outColor = determineMainColor(
		intersection.inside, intersection.distance, decodeColor(glyph.colorsAndSize.z)
	);
	if (glyph.colorsAndSize.w != 0 && glyph.strokeWidth > 0.0) {
		outColor = mixStrokeColor(
			outColor, decodeColor(glyph.colorsAndSize.w), determineStrokeIntensity(intersection.distance, glyph.strokeWidth)
		);
	}
}
