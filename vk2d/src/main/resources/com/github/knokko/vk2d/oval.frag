#version 450

#include "oval.glsl"

layout(location = 0) in vec2 position;
layout(location = 1) in flat Oval oval;

layout(location = 0) out vec4 outColor;

#include "decode.glsl"

void main() {
	vec2 difference = (vec2(oval.centerX, oval.centerY) - position) / vec2(oval.radiusX, oval.radiusY);
	float distance = difference.x * difference.x + difference.y * difference.y;

	uint rawColor0 = oval.centerColor;
	float distance0 = 0.0;
	uint rawColor1 = oval.color0;
	float distance1 = oval.distance0;

	if (distance >= oval.distance0) {
		rawColor0 = oval.color0;
		distance0 = oval.distance0;
		rawColor1 = oval.color1;
        distance1 = oval.distance1;
	}

	if (distance >= oval.distance1) {
		rawColor0 = oval.color1;
		distance0 = oval.distance1;
		rawColor1 = oval.color2;
		distance1 = oval.distance2;
	}

	if (distance >= oval.distance2) {
		rawColor0 = oval.color2;
		distance0 = oval.distance2;
		rawColor1 = oval.color3;
		distance1 = oval.distance3;
	}

	if (distance >= distance1 || rawColor0 == rawColor1) {
		outColor = decodeColor(rawColor1);
		return;
	}

	float slider = (distance - distance0) / (distance1 - distance0);
	outColor = slider * decodeColor(rawColor1) + (1.0 - slider) * decodeColor(rawColor0);
}
