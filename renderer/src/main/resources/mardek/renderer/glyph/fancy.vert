#version 450

layout(location = 0) in uint glyphIndex;

#include "info.glsl"

layout(set = 1, binding = 0) readonly buffer GlyphData {
	GlyphInfo glyphs[];
};

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out float relativeY;
layout(location = 2) out flat GlyphInfo glyph;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"

void main() {
	glyph = glyphs[glyphIndex];

	uint rawMinPosition = glyph.rawInfo.z;
	int minX = int(rawMinPosition & 0xFFFF) - 10000;
	int minY = int((rawMinPosition >> 16) & 0xFFFF) - 10000;

	int vertexX;
	int vertexY;

	uint rawVertex = gl_VertexIndex % 6;
	if (rawVertex >= 1 && rawVertex <= 3) {
		textureCoordinates.x = 1.0;
		vertexX = minX + int(glyph.colorsAndSize.x);
	} else {
		textureCoordinates.x = 0.0;
		vertexX = minX;
	}

	if (rawVertex >= 2 && rawVertex <= 4) {
		textureCoordinates.y = 1.0;
		vertexY = minY;
	} else {
		textureCoordinates.y = 0.0;
		vertexY = minY + int(glyph.colorsAndSize.y);
	}

	vec2 position = 2.0 * vec2(vertexX, vertexY) / viewportSize - vec2(1.0);
	gl_Position = vec4(position, 0.0, 1.0);
	relativeY = (glyph.yInfoAndStrokeWidth.y - vertexY) / glyph.yInfoAndStrokeWidth.z;
}
