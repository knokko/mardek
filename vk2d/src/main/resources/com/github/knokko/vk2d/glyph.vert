#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 2) in uint baseIndex;
layout(location = 3) in uint height;

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out flat uint propagateBaseIndex;
layout(location = 2) out flat uint propagateHeight;

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	propagateBaseIndex = baseIndex;
	propagateHeight = height;
}
