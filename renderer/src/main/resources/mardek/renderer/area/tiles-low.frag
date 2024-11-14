#version 450

#include "tiles.glsl"

float linearToSrgb(float linear) {
	if (linear <= 0.00313) return 12.92 * linear;
	else return 1.055 * pow(linear, 1.0 / 2.4) - 0.055;
}

vec3 linearToSrgb(vec3 linear) {
	return vec3(linearToSrgb(linear.r), linearToSrgb(linear.g), linearToSrgb(linear.b));
}

vec4 computeColor(uint packedTile, ivec2 tilePixel) {
	uint baseSpriteOffset = packedTile & 0xFFFFFFu;
	uint waterType = (packedTile >> 24u) & 7u;
	bool hasWaterAbove = ((packedTile >> 30u) & 1u) != 0u;

	vec4 waterColor = vec4(0.0);
	vec4 tileColor = sampleKim(baseSpriteOffset, tilePixel);

	if (waterType > 0) {
		vec4 waterBackgroundColor;
		if (hasWaterAbove) waterBackgroundColor = sampleKim(waterSpriteOffsets[0], tilePixel);
		else waterBackgroundColor = sampleKim(waterSpriteOffsets[1], tilePixel);

		vec4 waterBaseColor = sampleKim(waterSpriteOffsets[waterType], tilePixel);
		if (waterType == 2) {
			waterBaseColor.a = 0.3;
			waterColor = vec4(waterBaseColor.a * linearToSrgb(waterBaseColor.rgb) + (1.0 - waterBaseColor.a) * linearToSrgb(waterBackgroundColor.rgb), 1.0);
			waterColor = vec4(srgbToLinear(waterColor.rgb), 1.0);
		} else waterColor = waterBaseColor;
	}

	return vec4(tileColor.a * tileColor.rgb + (1.0 - tileColor.a) * waterColor.rgb, max(tileColor.a, waterColor.a));
}
