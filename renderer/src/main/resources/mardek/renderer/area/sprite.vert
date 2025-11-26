#version 450

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out flat uint textureIndex;
layout(location = 2) out flat uint header;
layout(location = 3) out flat uint firstColor;
layout(location = 4) out flat uint blinkColor;
layout(location = 5) out float opacity;

struct SpriteQuad {
	uint rawMinPosition;
	uint rawSize;
	uint textureIndex;
	uint rawBlinkColor;
	float opacity;
};

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(set = 1, binding = 0) readonly buffer SpriteQuads {
	SpriteQuad quads[];
};

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
	ivec2 scissorMin;
	ivec2 scissorBounds;
	uint firstIndex;
};

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "coordinates.glsl"

void main() {
	SpriteQuad quad = quads[firstIndex + gl_VertexIndex / 6];
	textureCoordinates = deriveTextureCoordinates(decodePosition(quad.rawMinPosition), decodePosition(quad.rawSize), scissorMin, scissorBounds);
	textureIndex = quad.textureIndex;
	header = textureData[textureIndex];
	firstColor = textureData[textureIndex + 1];
	blinkColor = quad.rawBlinkColor;
	opacity = quad.opacity;
}
