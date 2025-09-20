#version 450

layout(location = 0) in vec2 relativeCoordinates;
layout(location = 1) in flat uint rawColor;

layout(location = 0) out vec4 outColor;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"

void main() {
	vec4 lightColor = decodeColor(rawColor);

	float dx = 0.5 - relativeCoordinates.x;
	float dy = 0.5 - relativeCoordinates.y;
	float rawIntensity = 0.25 - dx * dx - dy * dy;
	if (rawIntensity > 0.0) {
		outColor = vec4(lightColor.rgb, rawIntensity * lightColor.a);
	} else outColor = vec4(0.0);
}
