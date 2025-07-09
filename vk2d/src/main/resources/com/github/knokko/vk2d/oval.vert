#version 450

layout(location = 0) in uint rawPosition;
layout(location = 1) in uint ovalIndex;

#include "decode.glsl"
#include "oval.glsl"

layout(set = 0, binding = 0) readonly buffer OvalData {
	Oval ovals[];
};

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

layout(location = 0) out vec2 position;
layout(location = 1) out flat Oval oval;

void main() {
	position = decodePosition(rawPosition, viewportSize);
	gl_Position = vec4(position, 0.0, 1.0);
	oval = ovals[ovalIndex];
}
