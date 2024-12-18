#version 450

layout(set = 0, binding = 0) readonly buffer MapBuffer {
	uint sprites[];
};

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat int textureOffset;
layout(location = 2) in float opacity;

layout(location = 0) out vec4 outColor;

#include "kim1.glsl"

defineReadInt(sprites)

defineSampleKimFloat(sprites)

void main() {
	vec4 baseColor = sampleKim(textureOffset, textureCoordinates);
	outColor = vec4(baseColor.rgb, opacity * baseColor.a);
}
