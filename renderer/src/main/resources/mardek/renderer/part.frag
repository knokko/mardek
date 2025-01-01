#version 450

layout(location = 0) in vec2 textureCoordinates;

layout(push_constant) uniform PushConstants {
	layout(offset = 32) int addColor;
	int multiplyColor;
};

layout(set = 0, binding = 0) uniform sampler2D textureSampler;

layout(location = 0) out vec4 outColor;

vec4 decodeColor(int rgba) {
	vec4 color;
	color.r = (rgba & 255) / 255.0;
	color.g = ((rgba >> 8) & 255) / 255.0;
	color.b = ((rgba >> 16) & 255) / 255.0;
	color.a = ((rgba >> 24) & 255) / 255.0;
	return color;
}

void main() {
	outColor = texture(textureSampler, textureCoordinates) * decodeColor(multiplyColor) + decodeColor(addColor);
}
