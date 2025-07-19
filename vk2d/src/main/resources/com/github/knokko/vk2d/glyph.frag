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
	for (; index < numIntersections; index++) {
		if (textureCoordinates.x < intersectionData[index + intersectionIndex]) break;
	}

	if (index % 2 == 0) outColor = vec4(0.0);
	else outColor = vec4(1.0);
}
