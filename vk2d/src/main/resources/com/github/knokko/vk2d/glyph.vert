#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 2) in uint horizontalIntersectionsIndex;
layout(location = 3) in uint verticalIntersectionsIndex;
layout(location = 4) in uvec2 size;
layout(location = 5) in uint rawFillColor;
layout(location = 6) in uint rawStrokeColor;
layout(location = 7) in uint rawBackgroundColor;

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out flat uint propagateHorizontalIntersections;
layout(location = 2) out flat uint propagateVerticalIntersections;
layout(location = 3) out flat uvec2 propagateSize;
layout(location = 4) out vec4 fillColor;
layout(location = 5) out vec4 strokeColor;
layout(location = 6) out vec4 backgroundColor;

#include "decode.glsl"

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	propagateHorizontalIntersections = horizontalIntersectionsIndex;
	propagateVerticalIntersections = verticalIntersectionsIndex;
	propagateSize = size;
	fillColor = decodeColor(rawFillColor);
	strokeColor = decodeColor(rawStrokeColor);
	backgroundColor = decodeColor(rawBackgroundColor);
}
