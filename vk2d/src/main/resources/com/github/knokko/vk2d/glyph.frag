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
layout(location = 9) in float strokeWidth;

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

float determineStrokeIntensity(float originalDistance) {
	// DESIRED BEHAVIOR:
	// When strokeWidth = 1.0:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 1.0 & 0.0
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.0 & 0.9 & 0.1
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.3 & 0.7 & 0.0
	// formula: alpha = 1.0 - distance

	// When strokeWidth = 0.5:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 0.5 & 0.0
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.0 & 0.45 & 0.05
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.15 & 0.35 & 0.0
	// formula: alpha = 0.5 - distance * 0.5

	// When strokeWidth = 0.1:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 0.1 & 0.0
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.0 & 0.09 & 0.01
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.07 & 0.03 & 0.0
	// formula: alpha = 0.1 - distance * 0.1

	// When strokeWidth = 2.0:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.5 & 1.0 & 0.5
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.4 & 1.0 & 0.6
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.8 & 1.0 & 0.2
	// - distance = 1.5 & 0.5 & 0.5 -> alpha = 0.0 & 1.0 & 1.0
	// formula: alpha = 1.5 - distance

	// When strokeWidth = 3.0:
	// - distance = 2.0 & 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 1.0 & 1.0 & 1.0
	// - distance = 1.1 & 0.1 & 0.9 & 1.9 -> alpha = 0.9 & 1.0 & 1.0 & 0.1
	// - distance = 1.7 & 0.7 & 0.3 & 1.3 -> alpha = 0.3 & 1.0 & 1.0 & 0.7
	// formula: alpha = 2.0 - distance

	if (strokeWidth >= 1.0) return clamp(0.5 + 0.5 * strokeWidth - originalDistance, 0.0, 1.0);
	else return clamp(strokeWidth - strokeWidth * originalDistance, 0.0, 1.0);
}

void main() {
	uint x = uint(intSize.x * textureCoordinates.x);
	uint y = uint(intSize.y * textureCoordinates.y);

	float waveX = recoverWavePosition(x, subpixelOffset.x, size.x);
	float waveY = recoverWavePosition(y, subpixelOffset.y, size.y);
	WaveIntersections horizontal = wave(horizontalInfoOffset, y, waveX);
	WaveIntersections vertical = wave(verticalInfoOffset, x, waveY);

	float horizontalDistance = horizontal.distance * size.x;
	float verticalDistance = vertical.distance * size.y;
	float originalDistance = min(horizontalDistance, verticalDistance);
	float distance = clamp(originalDistance, 0.0, 0.5);
	bool inside = horizontalDistance > verticalDistance ? horizontal.inside : vertical.inside;

	// The desired behavior is:
	// - inside && distance == 0 -> 0.5 * fillColor + 0.5 * background
	// - outside && distance == 0 -> 0.5 * fillColor + 0.5 * background
	// - inside && distance == 0.25 -> 0.75 * fillColor + 0.25 * background
	// - outside && distance == 0.25 -> 0.25 * fillColor + 0.75 * background
	// - inside && distance >= 0.5 -> 1.0 * fillColor + 0.0 * background
	// - outside && distance >= 0.5 -> 0.0 * fillColor + 1.0 * background
	vec4 mainColor;
	if (inside) mainColor = (0.5 + distance) * fillColor + (0.5 - distance) * backgroundColor;
	else mainColor = (0.5 - distance) * fillColor + (0.5 + distance) * backgroundColor;

	float strokeAlpha = strokeColor.a * determineStrokeIntensity(originalDistance);
	outColor = (1.0 - strokeAlpha) * mainColor + vec4(strokeAlpha * strokeColor.rgb, strokeAlpha);
}
