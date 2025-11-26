#version 450

layout(location = 0) in uint rawPosition;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 2) in uint infoIndex;

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out flat uint propagateTextureIndex;
layout(location = 2) out flat uint header;
layout(location = 3) out flat uint firstColor;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(set = 1, binding = 0) readonly buffer TextureInfo {
	uint textureInfo[];
};

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

#include "decode.glsl"

void main() {
	gl_Position = vec4(decodePosition(rawPosition, viewportSize), 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;

	propagateTextureIndex = textureInfo[3 * infoIndex];
	header = textureInfo[3 * infoIndex + 1];

	// Passing the first color to the fragment shader reduces the number of memory accesses needed by the fragment
	// shader, which boosts its performance. Note that the first color is the most used color.
	firstColor = textureInfo[3 * infoIndex + 2];
}
