#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint horizontalInfoOffset;
layout(location = 2) in flat uint verticalInfoOffset;
layout(location = 3) in flat uvec2 size;

layout(set = 0, binding = 0) readonly buffer IntersectionData {
	float intersectionData[];
};
layout(set = 0, binding = 1) readonly buffer IntersectionInfo {
	uint intersectionInfo[];
};

layout(location = 0) out vec4 outColor;

struct WaveIntersections {
	float distance;
	bool inside;
};

WaveIntersections wave(uint infoOffset, uint thisWave, float wavePosition) {
	uint infoIndex = infoOffset + 2 * thisWave;
	uint intersectionIndex = intersectionInfo[infoIndex];
	uint numIntersections = intersectionInfo[infoIndex + 1];

	uint index = 0;
	float curveDistance = 100000.0;
	float previousIntersection = -100000.0;
	for (; index < numIntersections; index++) {
		float nextIntersection = intersectionData[index + intersectionIndex];
		if (wavePosition < nextIntersection) {
			curveDistance = nextIntersection - wavePosition;
			break;
		}
		previousIntersection = nextIntersection;
	}

	if (index > 0) {
		curveDistance = min(curveDistance, wavePosition - previousIntersection);
	}

	WaveIntersections result;
	result.distance = curveDistance;
	result.inside = index % 2 == 1;
	return result;
}

void main() {
	uint x = uint(size.x * textureCoordinates.x);
	uint y = uint(size.y * textureCoordinates.y);

	WaveIntersections horizontal = wave(horizontalInfoOffset, y, textureCoordinates.x);
	WaveIntersections vertical = wave(verticalInfoOffset, x, textureCoordinates.y);

	vec4 strokeColor = vec4(1.0);
	vec4 fillColor = vec4(0.0, 0.0, 0.0, 1.0);

	vec4 outsideColor = vec4(0.2, 0.2, 0.2, 1.0);
	vec4 otherColor = strokeColor;

	if (horizontal.inside && vertical.inside) otherColor = fillColor;
	if (!horizontal.inside && !vertical.inside) otherColor = outsideColor;

	float distance = clamp(min(horizontal.distance * size.x, vertical.distance * size.y), 0.0, 1.0);
	outColor = (1.0 - distance) * strokeColor + distance * otherColor;
}
