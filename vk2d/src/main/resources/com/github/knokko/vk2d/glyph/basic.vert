#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 subpixelOffset;
layout(location = 2) in vec2 textureCoordinates;
layout(location = 3) in uint horizontalIntersectionsIndex;
layout(location = 4) in uint verticalIntersectionsIndex;
layout(location = 5) in vec2 size;
layout(location = 6) in uvec2 intSize;
layout(location = 7) in uint rawFillColor;
layout(location = 8) in uint rawStrokeColor;
layout(location = 9) in uint rawBackgroundColor;
layout(location = 10) in float strokeWidth;

#include "info.glsl"

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out flat GlyphInfo glyph;

#include "../decode.glsl"

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	glyph.subpixelOffset = subpixelOffset;
	glyph.horizontalInfoOffset = horizontalIntersectionsIndex;
	glyph.verticalInfoOffset = verticalIntersectionsIndex;
	glyph.renderRegionSize = size;
	glyph.intRegionSize = intSize;
	// TODO Decode in fragment shader?
	glyph.fillColor = decodeColor(rawFillColor);
	glyph.strokeColor = decodeColor(rawStrokeColor);
	glyph.backgroundColor = decodeColor(rawBackgroundColor);
	glyph.strokeWidth = strokeWidth;
}
