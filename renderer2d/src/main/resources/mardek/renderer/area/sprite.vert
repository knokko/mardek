#version 450

layout(location = 0) in uint rawPosition;
layout(location = 1) in uint rawSize;
layout(location = 2) in uint textureIndex;
layout(location = 3) in uint blinkColor;
layout(location = 4) in float opacity;

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out flat uint propagateTextureIndex;
layout(location = 2) out flat uint header;
layout(location = 3) out flat uvec4 firstColors;
layout(location = 4) out flat uint propagateBlinkColor;
layout(location = 5) out float propagateOpacity;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
	ivec2 scissorMin;
	ivec2 scissorBounds;
};

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"

void main() {
	ivec2 originalPosition = decodePosition(rawPosition);
	ivec2 size = decodePosition(rawSize);
	vec2 clampedPosition = clamp(originalPosition, scissorMin, scissorBounds);
	gl_Position = vec4(2.0 * clampedPosition / viewportSize - vec2(1.0), 0.0, 1.0);
	uint rawVertex = gl_VertexIndex % 6;

	float minU = max(0.0, float(clampedPosition.x - originalPosition.x) / size.x);
	float minV = max(0.0, float(clampedPosition.y - originalPosition.y) / size.y);
	float maxU = min(1.0, 1.0 - float(originalPosition.x - clampedPosition.x) / size.x);
	float maxV = min(1.0, 1.0 - float(originalPosition.y - clampedPosition.y) / size.y);
	textureCoordinates.x = rawVertex >= 1 && rawVertex <= 3 ? maxU : minU;
	textureCoordinates.y = rawVertex >= 2 && rawVertex <= 4 ? minV : maxV;

	propagateTextureIndex = textureIndex;
	header = textureData[textureIndex];
	firstColors = uvec4(
	    textureData[textureIndex + 1],
	    textureData[textureIndex + 2],
	    textureData[textureIndex + 3],
	    textureData[textureIndex + 4]
	);

	propagateBlinkColor = blinkColor;
	propagateOpacity = opacity;
}
