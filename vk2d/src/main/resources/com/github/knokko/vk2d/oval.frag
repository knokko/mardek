#version 450

#include "oval.glsl"

layout(location = 0) in vec2 position;
layout(location = 1) in flat Oval oval;

layout(location = 0) out vec4 outColor;

#include "decode.glsl"
#include "linear-gradients.glsl"

void main() {
	vec2 difference = (vec2(oval.centerX, oval.centerY) - position) / vec2(oval.radiusX, oval.radiusY);
	float distance = difference.x * difference.x + difference.y * difference.y;

	SelectedGradient selected = initialSelectedGradient(oval.centerColor, oval.color0, oval.distance0);
	selected = nextSelectedGradient(distance, selected, oval.color0, oval.color1, oval.distance0, oval.distance1);
	selected = nextSelectedGradient(distance, selected, oval.color1, oval.color2, oval.distance1, oval.distance2);
	selected = nextSelectedGradient(distance, selected, oval.color2, oval.color3, oval.distance2, oval.distance3);
	outColor = selectGradientColor(distance, selected);
}
