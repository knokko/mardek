#version 450

#include "tiles.glsl"

vec4 computeColor(uint packedTile, ivec2 tilePixel) {
	uint midTileOffset = packedTile & 0xFFFFu;
	uint highTileOffset = (packedTile >> 16u) & 0xFFFFu;

	vec4 midColor = vec4(0.0);
	if (midTileOffset != 0xFFFFu) midColor = sampleKim(GENERAL_SPRITES_SIZE + midTileOffset, tilePixel);

	vec4 highColor = vec4(0.0);
	if (highTileOffset != 0xFFFFu) highColor = sampleKim(GENERAL_SPRITES_SIZE + highTileOffset, tilePixel);

	return vec4(highColor.a * highColor.rgb + (1.0 - highColor.a) * midColor.rgb, max(midColor.a, highColor.a));
}
