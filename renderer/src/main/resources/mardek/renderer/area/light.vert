#version 450

layout(location = 0) out vec2 relativeCoordinates;
layout(location = 1) out flat uint color;

struct LightQuad {
	uint rawMinPosition;
	uint radius;
	uint rawColor;
};

layout(set = 0, binding = 0) readonly buffer LightQuads {
	LightQuad quads[];
};

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
	ivec2 scissorMin;
	ivec2 scissorBounds;
	uint firstIndex;
};

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "coordinates.glsl"

void main() {
	LightQuad quad = quads[firstIndex + gl_VertexIndex / 6];
	relativeCoordinates = deriveTextureCoordinates(decodePosition(quad.rawMinPosition), ivec2(2 * quad.radius), scissorMin, scissorBounds);
	color = quad.rawColor;
}
