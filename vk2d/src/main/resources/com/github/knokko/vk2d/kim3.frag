#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint textureIndex;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(location = 0) out vec4 outColor;

void main() {
	uint header = textureData[textureIndex];
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

	uint color = textureData[textureIndex + 1 + tableIndex];
	uint ured = color & 255u;
	uint ugreen = (color >> 8) & 255u;
	uint ublue = (color >> 16) & 255u;
	uint ualpha = (color >> 24) & 255u;
	outColor = vec4(ured / 255.0, ugreen / 255.0, ublue / 255.0, ualpha / 255.0);
}
