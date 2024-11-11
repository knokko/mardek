#version 450

layout(constant_id = 0) const int GENERAL_SPRITES_SIZE = 12;
layout(constant_id = 1) const int HIGH_TILE_SPRITES_SIZE = 34;
layout(constant_id = 2) const int MAP_BUFFER_SIZE = 56;

layout(set = 0, binding = 0) readonly buffer MapBuffer {
	uint mapAndSprites[GENERAL_SPRITES_SIZE + HIGH_TILE_SPRITES_SIZE + MAP_BUFFER_SIZE];
};

layout(push_constant) uniform PushConstants {
	ivec2 mapSize;
	ivec2 screenSize;
	ivec2 cameraPosition;
	int scale;
	int mapOffset;
};

layout(location = 0) in vec2 floatPosition;

layout(location = 0) out vec4 outColor;

#include "kim1.glsl"

defineReadInt(mapAndSprites)

defineSampleKimInt(mapAndSprites)

void main() {
	ivec2 areaPixel = ivec2(screenSize * floatPosition) / 2 + cameraPosition;
	if (areaPixel.x < 0 || areaPixel.y < 0) discard;

	int tileSize = 16 * scale;
	ivec2 tile = areaPixel / tileSize;
	if (tile.x >= mapSize.x || tile.y >= mapSize.y) discard;

	ivec2 tilePixel = (areaPixel % tileSize) / scale;
	uint packedTile = mapAndSprites[GENERAL_SPRITES_SIZE + HIGH_TILE_SPRITES_SIZE + mapOffset + tile.x + mapSize.x * tile.y];
	uint midTileOffset = packedTile & 0xFFFFu;
	uint highTileOffset = (packedTile >> 16u) & 0xFFFFu;

	vec4 midColor = vec4(0.0);
	if (midTileOffset != 0xFFFFu) midColor = sampleKim(GENERAL_SPRITES_SIZE + midTileOffset, tilePixel);

	vec4 highColor = vec4(0.0);
	if (highTileOffset != 0xFFFFu) highColor = sampleKim(GENERAL_SPRITES_SIZE + highTileOffset, tilePixel);

	outColor = vec4(highColor.a * highColor.rgb + (1.0 - highColor.a) * midColor.rgb, max(midColor.a, highColor.a));
}
