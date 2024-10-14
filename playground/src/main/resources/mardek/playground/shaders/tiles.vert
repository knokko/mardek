#version 450

layout(location = 0) out vec2 floatPosition;
vec2 quadPositions[] = { vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(1.0, 1.0), vec2(1.0, 1.0), vec2(-1.0, 1.0), vec2(-1.0, -1.0) };

void main() {
	gl_Position = vec4(quadPositions[gl_VertexIndex], 0.0, 1.0);
	floatPosition = 0.5 + 0.5 * quadPositions[gl_VertexIndex];
}
