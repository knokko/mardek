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
	uint x = uint(glyph.intRenderWidth * textureCoordinates.x);
	uint y = uint(glyph.intRenderHeight * textureCoordinates.y);

	float waveX = recoverWavePosition(x, glyph.subpixelX, glyph.renderWidth);
	float waveY = recoverWavePosition(y, glyph.subpixelY, glyph.renderHeight);
	WaveIntersection horizontal = wave(glyph.horizontalInfoOffset, y, waveX);
	WaveIntersection vertical = wave(glyph.verticalInfoOffset, x, waveY);

	float horizontalDistance = horizontal.distance * glyph.renderWidth;
	float verticalDistance = vertical.distance * glyph.renderHeight;

	WaveIntersection result;
	result.distance = min(horizontalDistance, verticalDistance);
	result.inside = horizontalDistance > verticalDistance ? horizontal.inside : vertical.inside;
	return result;
}
