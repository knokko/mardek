#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint horizontalInfoOffset;
layout(location = 2) in flat uint verticalInfoOffset;
layout(location = 3) in flat uvec2 size;
layout(location = 4) in vec4 fillColor;
layout(location = 5) in vec4 strokeColor;
layout(location = 6) in vec4 backgroundColor;

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

// Mimic the `computeWavePosition` of `glyph-scratch.comp`
float recoverWavePosition(uint thisWave, uint numWaves) {
	return (thisWave + 0.5) / numWaves;
}

void main() {
	uint x = uint(size.x * textureCoordinates.x);
	uint y = uint(size.y * textureCoordinates.y);

	WaveIntersections horizontal = wave(horizontalInfoOffset, y, recoverWavePosition(x, size.x));
	WaveIntersections vertical = wave(verticalInfoOffset, x, recoverWavePosition(y, size.y));

	float horizontalDistance = horizontal.distance * size.x;
	float verticalDistance = vertical.distance * size.y;
	//verticalDistance = horizontalDistance;
	//horizontalDistance = verticalDistance;
	float distance = clamp(min(horizontalDistance, verticalDistance), 0.0, 0.5);

	bool inside = horizontalDistance > verticalDistance ? horizontal.inside : vertical.inside;
	//inside = horizontal.inside;
	vec4 mainColor, otherColor;
	if (inside) {
		mainColor = fillColor;
		otherColor = backgroundColor;
	} else {
		mainColor = backgroundColor;
		otherColor = fillColor;
	}
	if (horizontal.inside != vertical.inside) mainColor = vec4(1.0, 0.0, 0.0, 1.0);

	// distance == 0 -> 0.5 * mainColor + 0.5 * otherColor
	// distance == 0.25 -> 0.75 * mainColor + 0.25 * otherColor
	// distance => 0.5 -> 1.0 * mainColor + 0.0 * otherColor
	outColor = (0.5 + distance) * mainColor + (0.5 - distance) * otherColor;
}
