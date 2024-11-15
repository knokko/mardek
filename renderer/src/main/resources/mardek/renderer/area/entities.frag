#version 450

#include "base.glsl"

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat int textureOffset;

layout(location = 0) out vec4 outColor;

#include "kim1.glsl"

defineReadInt(mapAndSprites)

defineSampleKimFloat(mapAndSprites)

void main() {
	outColor = sampleKim(textureOffset, textureCoordinates);
}
