#version 450

layout(constant_id = 0) const int MAP_WIDTH = 10;
layout(constant_id = 1) const int MAP_HEIGHT = 10;

layout(set = 0, binding = 0) uniform texture2DArray tileImages;
layout(set = 0, binding = 1) uniform sampler tileSampler;
layout(set = 0, binding = 2) readonly buffer MapBuffer {
	int mapBuffer[MAP_WIDTH * MAP_HEIGHT];
};

layout(push_constant) uniform PushConstants {
	int screenWidth;
	int screenHeight;
	int offsetX;
	int offsetY;
};

layout(location = 0) in vec2 floatPosition;

layout(location = 0) out vec4 outColor;

void main() {
	int screenPixelX = int(screenWidth * floatPosition.x) - offsetX;
	int screenPixelY = int(screenHeight * floatPosition.y) - offsetY;
	int tilePixelX = screenPixelX % 16;
	int tilePixelY = screenPixelY % 16;
	int tileX = screenPixelX / 16;
	int tileY = screenPixelY / 16;
	if (tileX >= MAP_WIDTH || tileY >= MAP_HEIGHT) discard;

	float u = (0.5 + tilePixelX) / 16.0;
	float v = (0.5 + tilePixelY) / 16.0;
	int layer = mapBuffer[tileX + MAP_WIDTH * tileY];
	outColor = texture(sampler2DArray(tileImages, tileSampler), vec3(u, v, layer));
}
