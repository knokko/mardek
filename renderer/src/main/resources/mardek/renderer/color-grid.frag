#version 450

#include "srgb.glsl"

vec3 itemColors[5] = {
	srgbToLinear(vec3(193.0 / 255.0, 145.0 / 255.0, 89.0 / 255.0)), // base item color
	srgbToLinear(vec3(81.0 / 255.0, 113.0 / 255.0, 217.0 / 255.0)), // expendable item color
	srgbToLinear(vec3(224.0 / 255.0, 128.0 / 255.0, 80.0 / 255.0)), // weapon color
	srgbToLinear(vec3(145.0 / 255.0, 209.0 / 255.0, 89.0 / 255.0)), // armor color
	srgbToLinear(vec3(209.0 / 255.0, 209.0 / 255.0, 89.0 / 255.0)) // accessory color
};

layout(location = 0) in vec2 textureCoordinates;

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
	uvec2 renderOffset;
	uvec2 tableSize;
	uint tableOffset;
	uint table;
	uint scale;
};

layout(set = 0, binding = 0) readonly buffer ImageBuffer {
	uint indexBuffer[];
};

layout(location = 0) out vec4 outColor;

void main() {
	uvec2 intCoords = uvec2(textureCoordinates * tableSize);
	uint rawTableIndex = 32 * tableOffset + 4 * (intCoords.x + tableSize.x * intCoords.y);
	uint rawColorIndex = indexBuffer[rawTableIndex / 32];
	uint localColorIndex = (rawColorIndex >> (rawTableIndex % 32)) & 15u;

	if (localColorIndex == 0) discard;

	vec3 color;
	if (table == 0) color = itemColors[localColorIndex - 1];

	outColor = vec4(color, 1.0);
}
