#version 450

layout(push_constant) uniform PushConstants {
	vec2 corners[4];
};
layout(location = 0) out vec2 textureCoordinates;

int vertexIndexMapping[] = { 0, 1, 2, 2, 3, 0 };
vec2 textureCoordinateMapping[] = { vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 1.0) };

void main() {
	int index = vertexIndexMapping[gl_VertexIndex];

	// Note: it is NOT valid to just use `vec2 corner = corners[index]` since push constants must be
	// indexed uniformly
	vec2 copyOfCorners[4] = corners;
	vec2 corner = copyOfCorners[index];

	gl_Position = vec4(corner, 0.0, 1.0);
	textureCoordinates = textureCoordinateMapping[index];
}
