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

WaveIntersection wave(uvec3 info, float wavePosition) {
	uint intersectionIndex = info.x;
	uint numActualIntersections = info.y;

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

	WaveIntersection result;
	result.inside = index % 2 == 1;
	result.distance = curveDistance;
	return result;
}

float improveDistanceUsingAlmostIntersections(
	float curveDistance, uvec3 horizontalInfo, uvec3 verticalInfo,
	float horizontalPosition, float verticalPosition,
	float horizontalScale, float verticalScale
) {
	float nearbyThreshold = 5.0;
	uint intersectionIndex = horizontalInfo.x + horizontalInfo.y;
	uint numAlmostIntersections = horizontalInfo.z;
	for (uint nearbyIndex = 0; nearbyIndex < numAlmostIntersections && curveDistance > nearbyThreshold; nearbyIndex++) {
		float parallelDistance = horizontalScale * (intersectionData[intersectionIndex + 2 * nearbyIndex] - horizontalPosition);
		if (parallelDistance > curveDistance) break;
		if (parallelDistance < -curveDistance) continue;
		float orthogonalDistance = abs(intersectionData[intersectionIndex + 2 * nearbyIndex + 1]);
		float distance = abs(parallelDistance) + horizontalScale * orthogonalDistance;
		curveDistance = min(distance, curveDistance);
	}

	if (curveDistance <= nearbyThreshold) return curveDistance;

	intersectionIndex = verticalInfo.x + verticalInfo.y;
    numAlmostIntersections = verticalInfo.z;
    for (uint nearbyIndex = 0; nearbyIndex < numAlmostIntersections && curveDistance > nearbyThreshold; nearbyIndex++) {
		float parallelDistance = verticalScale * (intersectionData[intersectionIndex + 2 * nearbyIndex] - verticalPosition);
		if (parallelDistance > curveDistance) break;
		if (parallelDistance < -curveDistance) continue;
		float orthogonalDistance = abs(intersectionData[intersectionIndex + 2 * nearbyIndex + 1]);
		float distance = abs(parallelDistance) + verticalScale * orthogonalDistance;
		curveDistance = min(distance, curveDistance);
	}
	return curveDistance;
}

// Mimic the `computeWavePosition` of `glyph-scratch.comp`
float recoverWavePosition(uint thisWave, float subpixelOffset, float size) {
	return (thisWave + 0.5 + subpixelOffset) / size;
}

WaveIntersection closestIntersection(GlyphInfo glyph, bool useAlmostIntersections) {
	uint x = uint(glyph.colorsAndSize.x * textureCoordinates.x);
	uint y = uint(glyph.colorsAndSize.y * textureCoordinates.y);

	float waveX = recoverWavePosition(x, glyph.subpixelAndSize.x, glyph.subpixelAndSize.z);
	float waveY = recoverWavePosition(y, glyph.subpixelAndSize.y, glyph.subpixelAndSize.w);

	uvec3 horizontalInfo = uvec3(
		intersectionInfo[glyph.rawInfo.x + 3 * y],
		intersectionInfo[glyph.rawInfo.x + 3 * y + 1],
		0
	);
	WaveIntersection horizontal = wave(horizontalInfo, waveX);

	uvec3 verticalInfo = uvec3(
		intersectionInfo[glyph.rawInfo.y + 3 * x],
		intersectionInfo[glyph.rawInfo.y + 3 * x + 1],
		0
	);
	WaveIntersection vertical = wave(verticalInfo, waveY);

	float horizontalDistance = horizontal.distance * glyph.subpixelAndSize.z;
	float verticalDistance = vertical.distance * glyph.subpixelAndSize.w;

	WaveIntersection result;
	result.distance = min(horizontalDistance, verticalDistance);
	result.inside = horizontalDistance > verticalDistance ? horizontal.inside : vertical.inside;

	if (!result.inside && result.distance > 1.5 && useAlmostIntersections) {
		horizontalInfo.z = intersectionInfo[glyph.rawInfo.x + 3 * y + 2];
		verticalInfo.z = intersectionInfo[glyph.rawInfo.y + 3 * x + 2];
		result.distance = improveDistanceUsingAlmostIntersections(
			result.distance, horizontalInfo, verticalInfo,
			waveX, waveY, glyph.subpixelAndSize.z, glyph.subpixelAndSize.w
		);
	}
	return result;
}
