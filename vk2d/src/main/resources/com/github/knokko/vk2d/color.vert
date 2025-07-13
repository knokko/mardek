#version 450

layout(location = 0) in uint rawPosition;
layout(location = 1) in uint rawColor;

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

layout(location = 0) out vec4 color;

#include "decode.glsl"

void main() {
	uint rawX = rawPosition & 0xFFFF;
	uint rawY = (rawPosition >> 16) & 0xFFFF;
	gl_Position = vec4(2.0 * vec2(rawX, rawY) / viewportSize - vec2(1.0), 0.0, 1.0);
	color = decodeColor(rawColor);
}
