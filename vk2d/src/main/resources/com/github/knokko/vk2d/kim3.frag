#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint textureIndex;
layout(location = 2) in flat uint header;
layout(location = 3) in flat uvec4 firstColors;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(location = 0) out vec4 outColor;

#include "decode.glsl"
#include "kim3.glsl"

defineSampleKim3(textureData)

void main() {
	outColor = sampleKim3(header, textureIndex, firstColors, textureCoordinates);
}
