#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 center;
layout(location = 2) in vec2 radius;
layout(location = 3) in flat uint centerColor;
layout(location = 4) in flat uvec4 colors;
layout(location = 5) in vec4 colorDistances;

layout(location = 0) out vec4 outColor;

#include "decode.glsl"

void main() {
	vec2 difference = (center - position) / radius;
	float distance = difference.x * difference.x + difference.y * difference.y;

	uint rawColor0 = centerColor;
	float distance0 = 0.0;
	uint rawColor1 = colors.x;
	float distance1 = colorDistances.x;

	if (distance >= colorDistances.x) {
		rawColor0 = colors.x;
		distance0 = colorDistances.x;
		rawColor1 = colors.y;
        distance1 = colorDistances.y;
	}

	if (distance >= colorDistances.y) {
		rawColor0 = colors.y;
		distance0 = colorDistances.y;
		rawColor1 = colors.z;
		distance1 = colorDistances.z;
	}

	if (distance >= colorDistances.z) {
		rawColor0 = colors.z;
		distance0 = colorDistances.z;
		rawColor1 = colors.w;
		distance1 = colorDistances.w;
	}

	if (distance >= distance1 || rawColor0 == rawColor1) {
		outColor = decodeColor(rawColor1);
		return;
	}

	float slider = (distance - distance0) / (distance1 - distance0);
	outColor = slider * decodeColor(rawColor1) + (1.0 - slider) * decodeColor(rawColor0);
}
