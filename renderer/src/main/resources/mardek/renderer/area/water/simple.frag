#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint rawTilePosition;
layout(location = 2) in flat uint backgroundTextureIndex;
layout(location = 3) in flat uint backgroundHeader;
layout(location = 4) in flat uint firstBackgroundColor;
layout(location = 5) in flat uint waterTextureIndex;
layout(location = 6) in flat uint waterHeader;
layout(location = 7) in flat uint firstWaterColor;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(location = 0) out vec4 outColor;

layout(push_constant) uniform PushConstants {
	uint microTime;
};

#include "../../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "../../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/kim3.glsl"
#include "./psrdnoise3.glsl"

defineSampleKim3(textureData)

vec2 modTextureCoordinates(vec2 coordinates) {
	if (coordinates.x < 0.0) coordinates.x += 1.0;
	if (coordinates.x >= 1.0) coordinates.x -= 1.0;
	if (coordinates.y < 0.0) coordinates.y += 1.0;
	if (coordinates.y >= 1.0) coordinates.y -= 1.0;
	return coordinates;
}

float srgbToLinear(float srgb) {
	if (srgb <= 0.04) return srgb / 12.92;
	else return pow((srgb + 0.055) / 1.055, 2.4);
}

float linearToSrgb(float linear) {
	if (linear < 0.00313) return 12.92 * linear;
	else return 1.055 * pow(linear, 1 / 2.4) - 0.055;
}

vec3 srgbToLinear(vec3 srgb) {
	return vec3(srgbToLinear(srgb.r), srgbToLinear(srgb.g), srgbToLinear(srgb.b));
}

vec3 linearToSrgb(vec3 linear) {
	return vec3(linearToSrgb(linear.r), linearToSrgb(linear.g), linearToSrgb(linear.b));
}

void main() {
	ivec2 tilePosition = decodePosition(rawTilePosition);
	vec2 realPosition = tilePosition + textureCoordinates;
	float noiseInputTime = microTime * 0.001 * 0.001 * 0.15;
	vec3 noiseInput = vec3(realPosition, noiseInputTime);
	vec3 noiseGradient;
	float noise1 = psrdnoise(vec3(0.1, 0.6, 1.0) * noiseInput, vec3(288, 288, 288), 0.0, noiseGradient);
	float noise2 = psrdnoise(vec3(1.0, 1.0, 3.0) * noiseInput + vec3(50, 140, 200), vec3(255, 255, 255), 0.0, noiseGradient);
	float noise3 = psrdnoise(vec3(0.15, 0.6, 0.8) * noiseInput - vec3(50, 140, 200), vec3(225, 225, 225), 0.0, noiseGradient);

	vec2 backgroundTextureCoordinates = modTextureCoordinates(textureCoordinates + vec2(0.1 * noise3, 0.0));
	vec4 backgroundColor = sampleKim3(backgroundHeader, backgroundTextureIndex, firstBackgroundColor, backgroundTextureCoordinates);
	vec2 waterTextureCoordinates = modTextureCoordinates(textureCoordinates + vec2(0.15 * noise1, 0.08 * noise3));
	vec4 waterColor = sampleKim3(waterHeader, waterTextureIndex, firstWaterColor, waterTextureCoordinates);
	float opacity = 0.3 + 0.04 * noise2;
	outColor = vec4(srgbToLinear((1.0 - opacity) * linearToSrgb(backgroundColor.rgb) + opacity * linearToSrgb(waterColor.rgb)), 1.0);
}
