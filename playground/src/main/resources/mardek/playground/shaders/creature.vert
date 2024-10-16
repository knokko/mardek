#version 450

layout(location = 0) in vec2 inPosition;

layout(location = 0) out int outTextureIndex;
layout(location = 1) out vec2 outTextureCoordinates;

vec2 textureCoordinates[] = { vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0), vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0) };

void main() {
	gl_Position = vec4(inPosition, 0.0, 1.0);
	outTextureIndex = gl_VertexIndex / 6;
	outTextureCoordinates = textureCoordinates[gl_VertexIndex % 6];
}
