#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint baseIndex;
layout(location = 2) in flat uint height;

layout(set = 0, binding = 0) readonly buffer IntersectionData {
	float intersectionData[];
};
layout(set = 0, binding = 1) readonly buffer IntersectionInfo {
	uint intersectionInfo[];
};

layout(location = 0) out vec4 outColor;

void main() {
	uint y = uint(height * textureCoordinates.y);
	uint infoIndex = baseIndex + 2 * y;
	uint intersectionIndex = intersectionInfo[infoIndex];
	uint numIntersections = intersectionInfo[infoIndex + 1];

	uint index = 0;
	float curveDistance = 100000.0;
	float previousIntersection = -100000.0;
	for (; index < numIntersections; index++) {
		float nextIntersection = intersectionData[index + intersectionIndex];
		if (textureCoordinates.x < nextIntersection) {
			curveDistance = nextIntersection - textureCoordinates.x;
			break;
		}
		previousIntersection = nextIntersection;
	}

	if (index > 0) {
		curveDistance = min(curveDistance, textureCoordinates.x - previousIntersection);
	}

	if (index % 2 == 0) outColor = vec4(0.0);
	else outColor = vec4(1.0);

	outColor = vec4(vec3(curveDistance), 1.0);
}
