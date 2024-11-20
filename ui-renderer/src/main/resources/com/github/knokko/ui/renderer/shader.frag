#version 450

layout(origin_upper_left) in vec4 gl_FragCoord;

layout(push_constant) uniform pc {
	int framebufferWidth;
	int framebufferHeight;
};

layout(set = 0, binding = 1) readonly buffer sb {
	uint[] intensities;
};
layout(set = 0, binding = 2) readonly buffer sb2 {
	int[] extra;
};

layout(set = 0, binding = 3) uniform sampler pixelatedSampler;

layout(set = 1, binding = 0) uniform texture2D currentImage;

layout(location = 0) in flat ivec2 corner;
layout(location = 1) in flat ivec2 size;
layout(location = 2) in flat int type;
layout(location = 3) in flat int extraIndex;

layout(location = 4) in flat int bufferIndex;
layout(location = 5) in flat int sectionWidth;
layout(location = 6) in flat int scale;
layout(location = 7) in flat int colorIndex;
layout(location = 8) in flat int inputColor;
layout(location = 9) in flat int outlineWidth;

layout(location = 0) out vec4 outColor;

vec4 decodeColor(int rawColor) {
	int red = rawColor & 255;
	int green = (rawColor >> 8) & 255;
	int blue = (rawColor >> 16) & 255;
	int alpha = (rawColor >> 24) & 255;

	return vec4(red / 255.0, green / 255.0, blue / 255.0, alpha / 255.0);
}

vec4 applyGradient(vec4 oldColor, ivec2 offset, int gi) {
	ivec2 gMin = ivec2(extra[gi], extra[gi + 1]);
	ivec2 gSize = ivec2(extra[gi + 2], extra[gi + 3]);
	ivec2 gOffset = offset - gMin;
	if (gOffset.x >= 0 && gOffset.y >= 0 && gOffset.x < gSize.x && gOffset.y < gSize.y) {
		vec2 factors = gOffset / vec2(gSize);
		vec4 baseColor = decodeColor(extra[gi + 4]);
		vec4 rightColor = decodeColor(extra[gi + 5]);
		vec4 upColor = decodeColor(extra[gi + 6]);
		vec4 deltaX = rightColor - baseColor;
		vec4 deltaY = upColor - baseColor;
		return baseColor + factors.x * deltaX + (1.0 - factors.y) * deltaY;
	}
	return oldColor;
}

void drawImage(ivec2 offset) {
	outColor = texture(sampler2D(currentImage, pixelatedSampler), vec2(offset) / size);
}

void fillColor(ivec2 offset) {
	outColor = decodeColor(extra[extraIndex]);

	int numGradients = extra[extraIndex + 1];
	for (int index = 0; index < numGradients; index++) {
		outColor = applyGradient(outColor, offset, extraIndex + 2 + 7 * index);
	}
}

void drawText(ivec2 offset) {

	// This should not happen, but I prefer explicitly checking over undefined behavior
	if (offset.x < 0 || offset.y < 0 || offset.x >= size.x || offset.y >= size.y) {
		outColor = vec4(0.0, 0.0, 0.0, 0.0);
		return;
	}

	int intensityIndex = bufferIndex + offset.x / scale + (offset.y / scale) * sectionWidth;
	int rawIntensityIndex = intensityIndex / 4;
	int byteIntensityIndex = intensityIndex % 4;
	uint rawIntensity = intensities[rawIntensityIndex];
	uint intensity = (rawIntensity >> (8 * byteIntensityIndex)) & 255u;

	int rawColor = inputColor;
	if (intensity > 255 - outlineWidth) {
		rawColor = extra[colorIndex + 2 + 255 - intensity];
	}

	if (intensity == 255 - outlineWidth) intensity = 255;

	outColor = decodeColor(rawColor);
	outColor.a *= (intensity / 255.0);
}

void main() {
	int framebufferX = int(gl_FragCoord.x);
	int framebufferY = int(gl_FragCoord.y);
	ivec2 offset = ivec2(framebufferX, framebufferY) - corner;

	if (type == 1) drawImage(offset);
	if (type == 2) drawText(offset);
	if (type == 3) fillColor(offset);
}
