uint kim3ExtractFromEarlyColor(uint tableIndex, uvec4 firstColors) {
	// Believe it or not... this weird if-else snake boosts FPS because it avoids the need to fetch the first 4 colors
	// from the storage buffer. This is useful, since some GPUs don't like it when multiple fragments attempt to load
	// from the same memory address... and the first 4 colors often contain the most frequent colors.
	if (tableIndex == 0) return firstColors.x;
	if (tableIndex == 1) return firstColors.y;
	if (tableIndex == 2) return firstColors.z;
	return firstColors.w;
}

#define defineSampleKim3(kimBufferName) vec4 sampleKim3(uint header, uint firstIndex, uvec4 firstColors, vec2 textureCoordinates) {\
	uint width = header & 4095;\
	uint height = (header >> 12) & 4095;\
	uint numColors = (header >> 24) & 255;\
\
	uint x = clamp(int(textureCoordinates.x * width), 0, width - 1);\
	uint y = clamp(int(textureCoordinates.y * height), 0, height - 1);\
	uint halfByteOffset = x + y * width;\
	if (numColors > 16) halfByteOffset *= 2;\
\
	uint indirectIndex = firstIndex + 1 + numColors + halfByteOffset / 8;\
	uint packedColorIndex = kimBufferName[indirectIndex];\
	uint colorIndex = packedColorIndex >> (4 * (halfByteOffset % 8));\
	if (numColors > 16) colorIndex &= 255;\
	else colorIndex &= 15;\
\
	if (colorIndex <= 3) return decodeColor(kim3ExtractFromEarlyColor(colorIndex, firstColors));\
	else return decodeColor(kimBufferName[firstIndex + 1 + colorIndex]);\
}
