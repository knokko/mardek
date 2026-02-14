#version 450

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out uint rawTilePosition;
layout(location = 2) out uint backgroundTextureIndex;
layout(location = 3) out uint backgroundHeader;
layout(location = 4) out uint firstBackgroundColor;
layout(location = 5) out uint waterTextureIndex;
layout(location = 6) out uint waterHeader;
layout(location = 7) out uint firstWaterColor;

struct WaterQuad {
	uint rawTilePosition;
	uint rawCornerPosition;
	uint backgroundTextureIndex;
	uint waterTextureIndex;
};

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(set = 1, binding = 0) readonly buffer WaterQuads {
	WaterQuad waterQuads[];
};

layout(push_constant) uniform PushConstants {
	layout(offset = 4) uint scale;
	uvec2 viewportSize;
	ivec2 scissorMin;
	ivec2 scissorBounds;
	uint firstIndex;
};

#include "../../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "../coordinates.glsl"

void main() {
	WaterQuad quad = waterQuads[firstIndex + gl_VertexIndex / 6];
	textureCoordinates = deriveTextureCoordinates(decodePosition(quad.rawCornerPosition), ivec2(16 * scale, 16 * scale), scissorMin, scissorBounds);
	rawTilePosition = quad.rawTilePosition;
	backgroundTextureIndex = quad.backgroundTextureIndex;
	backgroundHeader = textureData[backgroundTextureIndex];
	firstBackgroundColor = textureData[quad.backgroundTextureIndex + 1];
	waterTextureIndex = quad.waterTextureIndex;
	waterHeader = textureData[quad.waterTextureIndex];
	firstWaterColor = textureData[quad.waterTextureIndex + 1];
}
