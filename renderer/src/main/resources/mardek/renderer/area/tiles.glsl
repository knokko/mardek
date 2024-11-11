#include "base.glsl"

layout(push_constant) uniform PushConstants {
	ivec2 mapSize;
	ivec2 screenSize;
	ivec2 cameraPosition;
	int scale;
	int mapOffset;
	int waterSpriteOffsets[5];
};

layout(location = 0) in vec2 floatPosition;

layout(location = 0) out vec4 outColor;

#include "kim1.glsl"

defineReadInt(mapAndSprites)

defineSampleKimInt(mapAndSprites)

vec4 computeColor(uint packedTile, ivec2 tilePixel);

void main() {
	ivec2 areaPixel = ivec2(screenSize * floatPosition) / 2 + cameraPosition;
	if (areaPixel.x < 0 || areaPixel.y < 0) discard;

	int tileSize = 16 * scale;
	ivec2 tile = areaPixel / tileSize;
	if (tile.x >= mapSize.x || tile.y >= mapSize.y) discard;

	ivec2 tilePixel = (areaPixel % tileSize) / scale;
	uint packedTile = mapAndSprites[GENERAL_SPRITES_SIZE + HIGH_TILE_SPRITES_SIZE + mapOffset + tile.x + mapSize.x * tile.y];
	outColor = computeColor(packedTile, tilePixel);
}
