#version 450

layout(location = 0) in uint rawPosition;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 2) in uint textureIndex;

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out flat uint propagateTextureIndex;
layout(location = 2) out flat uint header;
layout(location = 3) out flat uvec4 firstColors;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

#include "decode.glsl"

void main() {
	gl_Position = vec4(decodePosition(rawPosition, viewportSize), 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	propagateTextureIndex = textureIndex;
	header = textureData[textureIndex];

	// Passing the first 4 colors to the fragment shader reduces the number of memory accesses needed by the fragment
	// shader, which boosts its performance. Note that the first 4 colors often contain the most used colors.
	firstColors = uvec4(
	    textureData[textureIndex + 1],
	    textureData[textureIndex + 2],
	    textureData[textureIndex + 3],
	    textureData[textureIndex + 4]
	);
}
