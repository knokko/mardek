#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 2) in uint horizontalIntersectionsIndex;
layout(location = 3) in uint verticalIntersectionsIndex;
layout(location = 4) in uvec2 size;

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out flat uint propagateHorizontalIntersections;
layout(location = 2) out flat uint propagateVerticalIntersections;
layout(location = 3) out flat uvec2 propagateSize;

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	propagateHorizontalIntersections = horizontalIntersectionsIndex;
	propagateVerticalIntersections = verticalIntersectionsIndex;
	propagateSize = size;
}
