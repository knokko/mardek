#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in vec4 addColor;
layout(location = 2) in vec4 multiplyColor;

layout(set = 0, binding = 0) uniform sampler2D textureSampler;

layout(location = 0) out vec4 outColor;

void main() {
	vec4 textureColor = texture(textureSampler, textureCoordinates);
	outColor = addColor + multiplyColor * textureColor;
}
