#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint textureIndex;
layout(location = 2) in flat uint header;
layout(location = 3) in flat uvec4 firstColors;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(location = 0) out vec4 outColor;

#include "decode.glsl"

void main() {
	uint width = header & 4095;
	uint height = (header >> 12) & 4095;
	uint numColors = (header >> 24) & 255;

	uint x = clamp(int(textureCoordinates.x * width), 0, width - 1);
	uint y = clamp(int(textureCoordinates.y * height), 0, height - 1);
	uint halfByteOffset = x + y * width;
	if (numColors > 16) halfByteOffset *= 2;

	uint packedIndex = textureData[textureIndex + 1 + numColors + halfByteOffset / 8];
	uint tableIndex = packedIndex >> (4 * (halfByteOffset % 8));
	if (numColors > 16) tableIndex &= 255;
	else tableIndex &= 15;

	// Believe it or not... this weird if-else snake boosts FPS because it avoids the need to fetch the first 4 colors
	// from the storage buffer. This is useful, since some GPUs don't like it when multiple fragments attempt to load
	// from the same memory address... and the first 4 colors often contain the most frequent colors.
	uint color;
	if (tableIndex == 0) color = firstColors.x;
	else if (tableIndex == 1) color = firstColors.y;
	else if (tableIndex == 2) color = firstColors.z;
	else if (tableIndex == 3) color = firstColors.w;
	else color = textureData[textureIndex + 1 + tableIndex];

	outColor = decodeColor(color);
}
