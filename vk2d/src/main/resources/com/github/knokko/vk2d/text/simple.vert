#version 450

#include "simple-style.glsl"

struct GlyphQuad {
	float corner0x; // 0 -> 4 bytes
	float corner0y; // 4 -> 8 bytes
	float corner1x; // 8 -> 12 bytes
	float corner1y; // 12 -> 16 bytes
	uint textureCorner0; // 16 -> 20 bytes
	float textureCorner1x; // 20 -> 24 bytes
	float textureCorner1y; // 24 -> 28 bytes
	float heightA; // 28 -> 32 bytes
	uint styleIndex; // 32 -> 36 bytes
};

layout(set = 0, binding = 0) readonly buffer GlyphQuadBuffer {
	GlyphQuad glyphQuadBuffer[];
};

layout(set = 1, binding = 0) readonly buffer TextStyleBuffer {
	TextStyle textStyleBuffer[];
};

layout(push_constant) uniform PushConstants {
	vec2 atlasSize;
};

layout(location = 0) out vec3 outTextureCoordinatesAndHeightA;
layout(location = 1) out flat TextStyle outStyle;

void main() {
	GlyphQuad quad = glyphQuadBuffer[gl_VertexIndex / 6];
	uint positionIndex = gl_VertexIndex % 6;
	if (positionIndex == 0 || positionIndex == 3) outStyle = textStyleBuffer[quad.styleIndex];

	if (positionIndex == 3) positionIndex = 2;
	if (positionIndex == 4) positionIndex = 3;
	if (positionIndex == 5) positionIndex = 0;

	gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
	if (positionIndex == 0 || positionIndex == 3) {
		gl_Position.x = quad.corner0x;
		outTextureCoordinatesAndHeightA.x = decodePosition(quad.textureCorner0).x / atlasSize.x;
	} else {
		gl_Position.x = quad.corner1x;
		outTextureCoordinatesAndHeightA.x = quad.textureCorner1x;
	}
	if (positionIndex == 0 || positionIndex == 1) {
		gl_Position.y = quad.corner0y;
		outTextureCoordinatesAndHeightA.y = decodePosition(quad.textureCorner0).y / atlasSize.y;
	} else {
		gl_Position.y = quad.corner1y;
		outTextureCoordinatesAndHeightA.y = quad.textureCorner1y;
	}

	outTextureCoordinatesAndHeightA.z = quad.heightA;
}
