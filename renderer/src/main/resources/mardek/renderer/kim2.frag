#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint spriteOffset;
layout(location = 2) in float opacity;

layout(set = 0, binding = 0) readonly buffer ImageBuffer {
	uint kimBuffer[];
};

layout(location = 0) out vec4 outColor;

#include "kim2.glsl"

defineSampleKim2Float(kimBuffer)

void main() {
	vec4 baseColor = sampleKim2(spriteOffset, textureCoordinates);
	outColor = vec4(baseColor.rgb, opacity * baseColor.a);
}
