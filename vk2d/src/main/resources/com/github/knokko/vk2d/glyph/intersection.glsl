layout(set = 0, binding = 0) readonly buffer IntersectionData {
	float intersectionData[];
};
layout(set = 0, binding = 1) readonly buffer IntersectionInfo {
	uint intersectionInfo[];
};

struct WaveIntersection {
	float distance;
	bool inside;
};

WaveIntersection wave(uint infoOffset, uint thisWave, float wavePosition) {
	uint infoIndex = infoOffset + 3 * thisWave;
	uint intersectionIndex = intersectionInfo[infoIndex];
	uint numActualIntersections = intersectionInfo[infoIndex + 1];
	uint numAlmostIntersections = intersectionInfo[infoIndex + 2];

	uint index = 0;
	float curveDistance = 100000.0;
	float previousIntersection = -100000.0;
	for (; index < numActualIntersections; index++) {
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

	for (uint nearbyIndex = 0; nearbyIndex < numAlmostIntersections; nearbyIndex++) {
		float parallelDistance = intersectionData[numActualIntersections + intersectionIndex + 2 * nearbyIndex] - wavePosition;
		float orthogonalDistance = intersectionData[numActualIntersections + intersectionIndex + 2 * nearbyIndex + 1];
		float distance = abs(parallelDistance) + abs(orthogonalDistance);
		curveDistance = min(distance, curveDistance);
	}

	WaveIntersection result;
	result.distance = curveDistance;
	result.inside = index % 2 == 1;
	return result;
}

// Mimic the `computeWavePosition` of `glyph-scratch.comp`
float recoverWavePosition(uint thisWave, float subpixelOffset, float size) {
	return (thisWave + 0.5 + subpixelOffset) / size;
}

WaveIntersection closestIntersection(GlyphInfo glyph) {
	uint x = uint(glyph.colorsAndSize.x * textureCoordinates.x);
	uint y = uint(glyph.colorsAndSize.y * textureCoordinates.y);
	vec2 test = glyph.colorsAndSize.xy;

	float waveX = recoverWavePosition(x, glyph.subpixelAndSize.x, glyph.subpixelAndSize.z);
	float waveY = recoverWavePosition(y, glyph.subpixelAndSize.y, glyph.subpixelAndSize.w);
	WaveIntersection horizontal = wave(glyph.rawInfo.x, y, waveX);
	WaveIntersection vertical = wave(glyph.rawInfo.y, x, waveY);

	float horizontalDistance = horizontal.distance * glyph.subpixelAndSize.z;
	float verticalDistance = vertical.distance * glyph.subpixelAndSize.w;

	WaveIntersection result;
	result.distance = min(horizontalDistance, verticalDistance);
	result.inside = horizontalDistance > verticalDistance ? horizontal.inside : vertical.inside;
	return result;
}
