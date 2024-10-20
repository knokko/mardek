#version 450

layout(constant_id = 0) const int MAP_WIDTH = 10;
layout(constant_id = 1) const int MAP_HEIGHT = 10;

layout(set = 0, binding = 0) uniform texture2DArray waterImages;
layout(set = 0, binding = 1) uniform sampler waterSampler;
layout(set = 0, binding = 2) readonly buffer MapBuffer {
	int waterBuffer[MAP_WIDTH * MAP_HEIGHT];
};

layout(push_constant) uniform PushConstants {
	ivec2 screenSize;
	ivec2 cameraPosition;
	int scale;
};

layout(location = 0) in vec2 floatPosition;

layout(location = 0) out vec4 outColor;

float srgbToLinear(float srgb) {
	if (srgb <= 0.04) return srgb / 12.92;
	else return pow((srgb + 0.044) / 1.055, 2.4);
}

vec3 srgbToLinear(vec3 srgb) {
	return vec3(srgbToLinear(srgb.r), srgbToLinear(srgb.g), srgbToLinear(srgb.b));
}

float linearToSrgb(float linear) {
	if (linear <= 0.00313) return 12.92 * linear;
	else return 1.055 * pow(linear, 1.0 / 2.4) - 0.055;
}

vec3 linearToSrgb(vec3 linear) {
	return vec3(linearToSrgb(linear.r), linearToSrgb(linear.g), linearToSrgb(linear.b));
}

void main() {

	ivec2 areaPixel = ivec2(screenSize * floatPosition) / 2 + cameraPosition;
	if (areaPixel.x < 0 || areaPixel.y < 0) discard;

	int tileSize = 16 * scale;
	ivec2 tilePixel = (areaPixel % tileSize) / scale;
	ivec2 tile = areaPixel / tileSize;
	if (tile.x >= MAP_WIDTH || tile.y >= MAP_HEIGHT) discard;

	int waterType = waterBuffer[tile.x + MAP_WIDTH * tile.y];
	if (waterType == 0) discard;

	int backgroundTextureLayer = 0; // Default background
	if (tile.y > 0 && waterBuffer[tile.x + MAP_WIDTH * (tile.y - 1)] == 0) {
		backgroundTextureLayer = 1; // Water backwall background
	}

	vec2 backgroundTextureCoordinates = (tilePixel + vec2(0.5)) / 16.0;

	vec4 backgroundColor = texture(
			sampler2DArray(waterImages, waterSampler),
			vec3(backgroundTextureCoordinates, backgroundTextureLayer)
	);
	vec4 waterColor = texture(
			sampler2DArray(waterImages, waterSampler),
			vec3(backgroundTextureCoordinates, waterType) // TODO Animate the water
	);

	// For some reason, the tilesheet water texture is always opaque, even though the water is translucent
	// in the original game.
	waterColor.a = 0.3;

	vec3 blendedColor = (1.0 - waterColor.a) * linearToSrgb(backgroundColor.rgb) + waterColor.a * linearToSrgb(waterColor.rgb);
	outColor = vec4(srgbToLinear(blendedColor), max(backgroundColor.a, waterColor.a));
}
