#include "srgb.glsl"

uvec2 getKim2ImageSize(uint header) {
    uint mask = (1 << 15) - 1;
	return uvec2(header & mask, (header >> 15) & mask);
}

uint bitsPerPixelMapping[4] = { 1, 2, 4, 8 };

#define	defineSampleKim2Float(kimBufferName) defineSampleKim2(kimBufferName, vec2, clamp(int(textureCoordinates.x * size.x), 0, size.x - 1), clamp(int(textureCoordinates.y * size.y), 0, size.y - 1))

#define	defineSampleKim2Int(kimBufferName) defineSampleKim2(kimBufferName, uvec2, textureCoordinates.x, textureCoordinates.y)

#define defineSampleKim2(kimBufferName, textureCoordinatesType, computeX, computeY) vec4 sampleKim2(uint offset, textureCoordinatesType textureCoordinates) {\
	uint header = kimBufferName[offset];\
	uvec2 size = getKim2ImageSize(header);\
	uint color = kimBufferName[offset + 1];\
	uint bitsPerPixel = bitsPerPixelMapping[(header >> 30) & 3];\
\
	uint x = computeX;\
	uint y = computeY;\
\
    uint bitIndex = (x + y * size.x) * bitsPerPixel;\
    uint bitMask = (1 << bitsPerPixel) - 1;\
    uint stepSize = 255 / bitMask;\
    uint sharedColor = kimBufferName[offset + 2 + bitIndex / 32];\
    uint rawAlpha = (sharedColor >> (bitIndex % 32)) & ((1 << bitsPerPixel) - 1);\
    uint alpha = rawAlpha * stepSize;\
	uint ured = color & 255u;\
	uint ugreen = (color >> 8) & 255u;\
	uint ublue = (color >> 16) & 255u;\
	return vec4(srgbToLinear(vec3(ured / 255.0, ugreen / 255.0, ublue / 255.0)), alpha / 255.0);\
}
