float srgbToLinear(float srgb) {
	if (srgb <= 0.04) return srgb / 12.92;
	else return pow((srgb + 0.044) / 1.055, 2.4);
}

vec3 srgbToLinear(vec3 srgb) {
	return vec3(srgbToLinear(srgb.r), srgbToLinear(srgb.g), srgbToLinear(srgb.b));
}

uint unpack(uint packed, uint bitOffset, uint bitLength) {
	uint unpacked = packed >> bitOffset;
	if (bitLength < 32u) unpacked &= (1u << bitLength) - 1u;
	return unpacked;
}

uint leadingZeros(uint i) {
	// This one is basically ripped from the Java standard library
	if (i == 0) return 32;
	int n = 31;
	if (i >= 1 << 16) { n -= 16; i >>= 16; }
	if (i >= 1 <<  8) { n -=  8; i >>=  8; }
	if (i >= 1 <<  4) { n -=  4; i >>=  4; }
	if (i >= 1 <<  2) { n -=  2; i >>=  2; }
	return n - (i >> 1);
}

uint computeBitsPerPixel(uint numColors) {
	return 32 - leadingZeros(numColors - 1);
}

#define defineReadInt(kimBufferName) uint readInt(uint bitIndex, uint bitLength) {\
	uint intIndex1 = bitIndex / 32;\
	uint bitIndex1 = bitIndex % 32;\
	uint value1 = kimBufferName[intIndex1];\
	int bitLength2 = int(bitIndex1) + int(bitLength) - 32;\
	if (bitLength2 > 0) {\
		uint bitLength1 = bitLength - bitLength2;\
		uint value2 = kimBufferName[intIndex1 + 1];\
		return unpack(value1, 32 - bitLength1, bitLength1) | (unpack(value2, 0, bitLength2) << bitLength1);\
	} else {\
		return unpack(value1, bitIndex1, bitLength);\
	}\
}

#define	defineSampleKimFloat(kimBufferName) defineSampleKim(kimBufferName, vec2, clamp(int(textureCoordinates.x * width), 0, width - 1), clamp(int(textureCoordinates.y * height), 0, height - 1))

#define	defineSampleKimInt(kimBufferName) defineSampleKim(kimBufferName, uvec2, textureCoordinates.x, textureCoordinates.y)

#define defineSampleKim(kimBufferName, textureCoordinatesType, computeX, computeY) vec4 sampleKim(uint offset, textureCoordinatesType textureCoordinates) {\
	uint header = kimBufferName[offset];\
	uint width = unpack(header, 0, 10);\
	uint height = unpack(header, 10, 10);\
	uint numColors = unpack(header, 20, 10);\
	uint numChannels = 1 + unpack(header, 30, 2);\
\
	uint x = computeX;\
	uint y = computeY;\
\
	uint bitsPerPixel = computeBitsPerPixel(numColors);\
	uint colorIndex = readInt(32 * offset + 32 + 8 * numColors * numChannels + bitsPerPixel * (x + y * width), bitsPerPixel);\
\
	uint color = readInt(32 * offset + 32 + colorIndex * 8 * numChannels, 8 * numChannels);\
	uint ured = color & 255u;\
	uint ugreen = (color >> 8) & 255u;\
	uint ublue = (color >> 16) & 255u;\
	uint ualpha = (color >> 24) & 255u;\
	if (numChannels < 2) ugreen = ured;\
	if (numChannels < 3) ublue = ugreen;\
	if (numChannels < 4) ualpha = 255u;\
	return vec4(srgbToLinear(vec3(ured / 255.0, ugreen / 255.0, ublue / 255.0)), ualpha / 255.0);\
}
