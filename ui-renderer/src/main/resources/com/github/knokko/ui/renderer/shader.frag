#version 450

layout(push_constant) uniform pc {
    int quadBufferOffset;
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

layout(location = 0) in vec2 offset;
layout(location = 1) in flat ivec2 size;
layout(location = 2) in flat int type;
layout(location = 3) in flat int extraIndex;

layout(location = 4) in flat int bufferIndex;
layout(location = 5) in flat int sectionWidth;
layout(location = 6) in flat int scale;
layout(location = 7) in flat int colorIndex;
layout(location = 8) in flat int inputColor;
layout(location = 9) in flat int outlineWidth;
layout(location = 10) in vec2 absoluteOffset;

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

vec4 applyCircleGradient(vec4 oldColor, float distance, int gi) {
	float minDistance = extra[gi] * 0.001;
	float maxDistance = extra[gi + 1] * 0.001;
	if (distance >= minDistance && distance <= maxDistance) {
		float relative = (distance - minDistance) / (maxDistance - minDistance);
		vec4 minColor = decodeColor(extra[gi + 2]);
		vec4 maxColor = decodeColor(extra[gi + 3]);
		return relative * maxColor + (1.0 - relative) * minColor;
	}
	return oldColor;
}

void drawImage() {
	outColor = texture(sampler2D(currentImage, pixelatedSampler), vec2(offset) / size);
}

void fillColor() {
	outColor = decodeColor(extra[extraIndex]);

	int numGradients = extra[extraIndex + 1];
	for (int index = 0; index < numGradients; index++) {
		outColor = applyGradient(outColor, ivec2(offset), extraIndex + 2 + 7 * index);
	}
}

void fillCircle(ivec2 absoluteOffset) {
	vec2 offset = 2.0 * vec2(absoluteOffset) / vec2(size) - vec2(1.0, 1.0);
	float distance = sqrt(offset.x * offset.x + offset.y * offset.y);
	if (distance > 1.05) {
		outColor = vec4(0.0);
		return;
	}

	if (distance > 1.0) {
		float relative = (distance - 1.0) / 0.05;
		outColor = (1.0 - relative) * decodeColor(extra[extraIndex]);
		return;
	}

	outColor = decodeColor(extra[extraIndex]);

	int numGradients = extra[extraIndex + 1];
	for (int index = 0; index < numGradients; index++) {
		outColor = applyCircleGradient(outColor, distance, extraIndex + 2 + 4 * index);
	}
}

void drawText() {

	// This should not happen, but I prefer explicitly checking over undefined behavior
	if (offset.x < 0 || offset.y < 0 || offset.x >= size.x || offset.y >= size.y) {
		outColor = vec4(0.0, 0.0, 0.0, 0.0);
		return;
	}

	int intensityIndex = bufferIndex + int(offset.x) / scale + (int(offset.y) / scale) * sectionWidth;
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

	if (intensity > 0 && rawColor == inputColor) {
		int gradientIndex = colorIndex + 2 + outlineWidth;
		ivec2 gradientCorner = ivec2(extra[gradientIndex], extra[gradientIndex + 1]);
		int framebufferX = int(absoluteOffset.x);
		int framebufferY = int(absoluteOffset.y);
		ivec2 gradientOffset = ivec2(framebufferX, framebufferY) - gradientCorner;

		int numGradients = extra[gradientIndex + 2];
		for (int index = 0; index < numGradients; index++) {
			outColor = applyGradient(outColor, gradientOffset, gradientIndex + 3 + 7 * index);
		}
	}

	outColor.a *= (intensity / 255.0);
}

void main() {
	if (type == 1) drawImage();
	if (type == 2) drawText();
	if (type == 3) fillColor();
	if (type == 4) fillCircle(ivec2(offset));
}
