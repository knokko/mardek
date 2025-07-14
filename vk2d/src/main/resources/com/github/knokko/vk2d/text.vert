#version 450

layout (location = 0) in vec2 vertexPosition;
layout (location = 1) in vec2 vertexUV;
layout (location = 2) in int vertexIndex;

layout(location = 0) out vec2 uv;
layout(location = 1) flat out int bufferIndex;
layout(location = 2) out vec4 color;

void main() {
	gl_Position = vec4(vertexPosition, 0, 1);
	uv = vertexUV;
	bufferIndex = vertexIndex;
	color = vec4(1.0, 1.0, 0.0, 1.0);
}
