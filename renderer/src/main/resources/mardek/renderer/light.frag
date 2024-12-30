#version 450

layout(location = 0) in vec2 relativeCoordinates;
layout(location = 1) in flat uint rawColor;

layout(location = 0) out vec4 outColor;

void main() {
	uint ured = rawColor & 255u;
	uint ugreen = (rawColor >> 8) & 255u;
	uint ublue = (rawColor >> 16) & 255u;
	uint ualpha = (rawColor >> 24) & 255u;
	vec4 lightColor = vec4(ured / 255.0, ugreen / 255.0, ublue / 255.0, ualpha / 255.0);

	float dx = 0.5 - relativeCoordinates.x;
	float dy = 0.5 - relativeCoordinates.y;
	float rawIntensity = 0.25 - dx * dx - dy * dy;
	if (rawIntensity > 0.0) {
		outColor = vec4(lightColor.rgb, rawIntensity * lightColor.a);
	} else outColor = vec4(0.0);
}
