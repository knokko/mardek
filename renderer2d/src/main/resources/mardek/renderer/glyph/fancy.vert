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

	uint minX = glyph.rawMinPosition & 0xFFFF;
	uint minY = (glyph.rawMinPosition >> 16) & 0xFFFF;

	uint vertexX;
	uint vertexY;

	uint rawVertex = gl_VertexIndex % 6;
	if (rawVertex >= 1 && rawVertex <= 3) {
		textureCoordinates.x = 1.0;
		vertexX = minX + glyph.intRenderWidth;
	} else {
		textureCoordinates.x = 0.0;
		vertexX = minX;
	}

	if (rawVertex >= 2 && rawVertex <= 4) {
		textureCoordinates.y = 1.0;
		vertexY = minY;
	} else {
		textureCoordinates.y = 0.0;
		vertexY = minY + glyph.intRenderHeight;
	}

	vec2 position = 2.0 * vec2(vertexX, vertexY) / viewportSize - vec2(1.0);
	gl_Position = vec4(position, 0.0, 1.0);
	relativeY = (glyph.baseY - vertexY) / glyph.heightA;
}
