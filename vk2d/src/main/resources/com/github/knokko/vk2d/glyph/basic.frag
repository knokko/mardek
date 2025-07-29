#version 450

#include "info.glsl"

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat GlyphInfo glyph;

layout(location = 0) out vec4 outColor;

#include "intersection.glsl"
#include "color.glsl"

void main() {
	WaveIntersection intersection = closestIntersection(glyph);
	vec4 mainColor = determineMainColor(
		intersection.inside, intersection.distance, glyph.fillColor, glyph.backgroundColor
	);
	outColor = mixStrokeColor(
		mainColor, glyph.strokeColor, determineStrokeIntensity(intersection.distance, glyph.strokeWidth)
	);
}
