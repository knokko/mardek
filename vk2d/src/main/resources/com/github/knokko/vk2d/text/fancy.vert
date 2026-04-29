#version 450

#include "fancy-style.glsl"

struct GlyphQuad {
	float corner1x; // 0 -> 4 bytes
	float corner1y; // 4 -> 8 bytes
	float corner2x; // 8 -> 12 bytes
	float corner2y; // 12 -> 16 bytes
	float corner3x; // 16 -> 20 bytes
	float corner3y; // 20 -> 24 bytes
	float corner4x; // 24 -> 28 bytes
	float corner4y; // 28 -> 32 bytes
	uint textureCorner1; // 32 -> 36 bytes
	float textureCorner3x; // 36 -> 40 bytes
	float textureCorner3y; // 40 -> 44 bytes
	float heightA; // 44 -> 48 bytes
	float baseX; // 48 -> 52 bytes
	float baseY; // 52 -> 56 bytes
	uint styleIndex; // 56 -> 60 bytes
};

layout(set = 0, binding = 0) readonly buffer GlyphQuadBuffer {
	GlyphQuad glyphQuadBuffer[];
};

layout(set = 1, binding = 0) readonly buffer TextStyleBuffer {
	FancyTextStyle textStyleBuffer[];
};

layout(push_constant) uniform PushConstants {
	uvec2 atlasSize;
	uvec2 viewportSize;
};

layout(location = 0) out vec4 outVertexInfo;
layout(location = 1) out flat FancyTextStyle outStyle;

void main() {
	GlyphQuad quad = glyphQuadBuffer[gl_VertexIndex / 6];
	uint positionIndex = gl_VertexIndex % 6;
	if (positionIndex == 0 || positionIndex == 3) outStyle = textStyleBuffer[quad.styleIndex];

	if (positionIndex == 3) positionIndex = 2;
	if (positionIndex == 4) positionIndex = 3;
	if (positionIndex == 5) positionIndex = 0;

	gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
	if (positionIndex == 0) {
		gl_Position.x = quad.corner1x;
		gl_Position.y = quad.corner1y;
	}
	if (positionIndex == 1) {
		gl_Position.x = quad.corner2x;
		gl_Position.y = quad.corner2y;
	}
	if (positionIndex == 2) {
		gl_Position.x = quad.corner3x;
		gl_Position.y = quad.corner3y;
	}
	if (positionIndex == 3) {
		gl_Position.x = quad.corner4x;
		gl_Position.y = quad.corner4y;
	}

	if (positionIndex == 0 || positionIndex == 3) {
		outVertexInfo.x = decodePosition(quad.textureCorner1).x / float(atlasSize.x);
	} else {
		outVertexInfo.x = quad.textureCorner3x;
	}
	if (positionIndex == 0 || positionIndex == 1) {
		outVertexInfo.y = decodePosition(quad.textureCorner1).y / float(atlasSize.y);
	} else {
		outVertexInfo.y = quad.textureCorner3y;
	}

	outVertexInfo.z = quad.heightA;

	float baseX1 = 0.5 * viewportSize.x * quad.baseX;
	float baseY1 = 0.5 * viewportSize.y * quad.baseY;
	float baseX2 = 0.5 * viewportSize.x * (quad.baseX + (quad.corner2x - quad.corner1x));
	float baseY2 = 0.5 * viewportSize.y * (quad.baseY + (quad.corner2y - quad.corner1y));
	float renderX = 0.5 * viewportSize.x * gl_Position.x;
	float renderY = 0.5 * viewportSize.y * gl_Position.y;
	float distanceNumerator = (baseY2 - baseY1) * renderX - (baseX2 - baseX1) * renderY + baseX2 * baseY1 - baseY2 * baseX1;
	float distanceDenominator = length(vec2(baseX2, baseY2) - vec2(baseX1, baseY1));
	outVertexInfo.w = distanceNumerator / distanceDenominator;
	outVertexInfo.w /= quad.heightA;
}
