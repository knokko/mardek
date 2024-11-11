layout(constant_id = 0) const int GENERAL_SPRITES_SIZE = 12;
layout(constant_id = 1) const int HIGH_TILE_SPRITES_SIZE = 34;
layout(constant_id = 2) const int MAP_BUFFER_SIZE = 56;

layout(set = 0, binding = 0) readonly buffer MapBuffer {
	uint mapAndSprites[GENERAL_SPRITES_SIZE + HIGH_TILE_SPRITES_SIZE + MAP_BUFFER_SIZE];
};
