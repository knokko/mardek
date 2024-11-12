#version 450

layout(location = 0) in ivec2 offset;
layout(location = 1) in int imageIndex;

layout(push_constant) uniform PushConstants {
	ivec2 screenSize;
	ivec2 cameraOffset;
	int scale;
};

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out int propagateImageIndex;

vec2 textureCoordinateMapping[] = {
		vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
		vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0)
};

void main() {
	textureCoordinates = textureCoordinateMapping[gl_VertexIndex];
	vec2 relative = 2.0 * vec2(offset - cameraOffset) / vec2(screenSize);
	// TODO Support textures larger than 16x16
	gl_Position = vec4(relative + 32.0 * scale * textureCoordinates / vec2(screenSize), 0.0, 1.0);
	propagateImageIndex = imageIndex;
}
