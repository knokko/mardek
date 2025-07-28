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

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out vec2 propagateSubpixelOffset;
layout(location = 2) out flat uint propagateHorizontalIntersections;
layout(location = 3) out flat uint propagateVerticalIntersections;
layout(location = 4) out vec2 propagateSize;
layout(location = 5) out flat uvec2 propagateIntSize;
layout(location = 6) out vec4 fillColor;
layout(location = 7) out vec4 strokeColor;
layout(location = 8) out vec4 backgroundColor;
layout(location = 9) out float propagateStrokeWidth;

#include "decode.glsl"

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	propagateSubpixelOffset = subpixelOffset;
	propagateHorizontalIntersections = horizontalIntersectionsIndex;
	propagateVerticalIntersections = verticalIntersectionsIndex;
	propagateSize = size;
	propagateIntSize = intSize;
	fillColor = decodeColor(rawFillColor);
	strokeColor = decodeColor(rawStrokeColor);
	backgroundColor = decodeColor(rawBackgroundColor);
	propagateStrokeWidth = strokeWidth;
}
