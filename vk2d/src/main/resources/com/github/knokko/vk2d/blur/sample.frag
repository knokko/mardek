#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in vec4 addColor;
layout(location = 2) in vec4 multiplyColor;

layout(set = 0, binding = 0) readonly buffer InputBuffer {
	uint inputBuffer[];
};

layout(push_constant) uniform PushConstants {
	uvec2 textureSize;
};

layout(location = 0) out vec4 outColor;

#include "../decode.glsl"

void main() {
	uint x = clamp(int(textureCoordinates.x * textureSize.x), 0, textureSize.x - 1);
	uint y = clamp(int(textureCoordinates.y * textureSize.y), 0, textureSize.y - 1);
	vec4 textureColor = decodeColor(inputBuffer[y * textureSize.x + x]);
	outColor = addColor + multiplyColor * textureColor;
}
