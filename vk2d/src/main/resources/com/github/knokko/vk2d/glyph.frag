#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in vec2 subpixelOffset;
layout(location = 2) in flat uint horizontalInfoOffset;
layout(location = 3) in flat uint verticalInfoOffset;
layout(location = 4) in vec2 size;
layout(location = 5) in flat uvec2 intSize;
layout(location = 6) in vec4 fillColor;
layout(location = 7) in vec4 strokeColor;
layout(location = 8) in vec4 backgroundColor;

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
float recoverWavePosition(uint thisWave, float subpixelOffset, float size) {
	return (thisWave + 0.5 + subpixelOffset) / size;
}

void main() {
	uint x = uint(intSize.x * textureCoordinates.x);
	uint y = uint(intSize.y * textureCoordinates.y);

	float waveX = recoverWavePosition(x, subpixelOffset.x, size.x);
	float waveY = recoverWavePosition(y, subpixelOffset.y, size.y);
	WaveIntersections horizontal = wave(horizontalInfoOffset, y, waveX);
	WaveIntersections vertical = wave(verticalInfoOffset, x, waveY);

	float horizontalDistance = horizontal.distance * intSize.x;
	float verticalDistance = vertical.distance * intSize.y;
	float distance = clamp(min(horizontalDistance, verticalDistance), 0.0, 0.5);

	bool inside = horizontalDistance > verticalDistance ? horizontal.inside : vertical.inside;
	vec4 mainColor = inside ? fillColor : backgroundColor;

	// Without stroke (assuming strokeColor = 0.5 * (mainColor + otherColor), the desired behavior is:
	// - inside && distance == 0 -> 0.5 * fillColor + 0.5 * background = 1.0 * strokeColor + 0.0 * fillColor
	// - outside && distance == 0 -> 0.5 * fillColor + 0.5 * background = 1.0 * strokeColor + 0.0 * background
	// - inside && distance == 0.25 -> 0.75 * fillColor + 0.25 * background = 0.5 * strokeColor + 0.5 * fillColor
	// - outside && distance == 0.25 -> 0.25 * fillColor + 0.75 * background = 0.5 * strokeColor + 0.5 * background
	// - inside && distance >= 0.5 -> 1.0 * fillColor + 0.0 * background = 0.0 * strokeColor + 1.0 * fillColor
	// - outside && distance >= 0.5 -> 0.0 * fillColor + 1.0 * background = 0.0 * strokeColor + 1.0 * background
	//
	// In all cases, it should be equal to (1.0 - 2 * distance) * strokeColor + (2 * distance) * mainColor;
	outColor = (1.0 - 2.0 * distance) * strokeColor + 2.0 * distance * mainColor;
}
