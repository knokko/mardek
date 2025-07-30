#version 450

layout(location = 0) in uint rawPosition;
layout(location = 1) in uint rawColor;

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

layout(location = 0) out vec4 color;

#include "decode.glsl"

void main() {
	gl_Position = vec4(decodePosition(rawPosition, viewportSize), 0.0, 1.0);
	color = decodeColor(rawColor);
}
