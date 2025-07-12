#version 450

layout(location = 0) in vec2 textureCoordinates;

layout(set = 0, binding = 0) uniform sampler2D textureSampler;

layout(location = 0) out vec4 outColor;

void main() {
	outColor = texture(textureSampler, textureCoordinates);
}
