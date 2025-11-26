#define defineSampleKim3(kimBufferName) vec4 sampleKim3(uint header, uint firstIndex, uint firstColor, vec2 textureCoordinates) {\
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
	if (colorIndex == 0) return decodeColor(firstColor);\
	else return decodeColor(kimBufferName[firstIndex + 1 + colorIndex]);\
}
