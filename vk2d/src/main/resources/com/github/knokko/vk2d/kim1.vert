#version 450

layout(location = 0) in uint rawPosition;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 2) in uint textureIndex;

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out flat uint propagateTextureIndex;

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

#include "decode.glsl"

void main() {
	gl_Position = vec4(decodePosition(rawPosition, viewportSize), 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	propagateTextureIndex = textureIndex;
}
