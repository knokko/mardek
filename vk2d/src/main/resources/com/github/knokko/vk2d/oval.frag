#version 450

#include "oval.glsl"

layout(location = 0) in vec2 position;
layout(location = 1) in flat Oval oval;

layout(location = 0) out vec4 outColor;

#include "decode.glsl"
#include "linear-gradients.glsl"

void main() {
	vec2 difference = (oval.centerAndRadius.xy - position) / oval.centerAndRadius.zw;
	float distance = difference.x * difference.x + difference.y * difference.y;
	outColor = computeGradientColor(distance, oval.centerColor, oval.distances, oval.colors);
}
