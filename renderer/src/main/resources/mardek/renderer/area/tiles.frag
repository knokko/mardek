#version 450

layout(constant_id = 0) const int MAP_WIDTH = 10;
layout(constant_id = 1) const int MAP_HEIGHT = 10;

layout(set = 0, binding = 0) uniform texture2DArray tileImages;
layout(set = 0, binding = 1) uniform sampler tileSampler;
layout(set = 0, binding = 2) readonly buffer MapBuffer {
	int mapBuffer[MAP_WIDTH * MAP_HEIGHT];
};

layout(push_constant) uniform PushConstants {
	ivec2 screenSize;
	ivec2 cameraPosition;
	int scale;
};

layout(location = 0) in vec2 floatPosition;

layout(location = 0) out vec4 outColor;

void main() {
	ivec2 areaPixel = ivec2(screenSize * floatPosition) / 2 + cameraPosition;
	if (areaPixel.x < 0 || areaPixel.y < 0) discard;

	int tileSize = 16 * scale;
	ivec2 tilePixel = (areaPixel % tileSize) / scale;
	ivec2 tile = areaPixel / tileSize;
	if (tile.x >= MAP_WIDTH || tile.y >= MAP_HEIGHT) discard;

	vec2 textureCoordinates = (tilePixel + vec2(0.5)) / 16.0;
	int layer = mapBuffer[tile.x + MAP_WIDTH * tile.y];
	if (layer == -1) discard;

	outColor = texture(sampler2DArray(tileImages, tileSampler), vec3(textureCoordinates, layer));
}
