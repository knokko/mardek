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

void drawImage(ivec2 offset) {
	outColor = texture(sampler2D(currentImage, pixelatedSampler), vec2(offset) / size);
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

	int red = rawColor & 255;
	int green = (rawColor >> 8) & 255;
	int blue = (rawColor >> 16) & 255;
	int alpha = (rawColor >> 24) & 255;

	outColor = vec4(red / 255.0, green / 255.0, blue / 255.0, (alpha / 255.0) * intensity / 255.0);
}

void main() {
	int framebufferX = int(gl_FragCoord.x);
	int framebufferY = int(gl_FragCoord.y);
	ivec2 offset = ivec2(framebufferX, framebufferY) - corner;

	if (type == 1) drawImage(offset);
	if (type == 2) drawText(offset);
}
