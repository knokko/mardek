#version 450

layout(push_constant) uniform pc {
	int framebufferWidth;
	int framebufferHeight;
};
layout(set = 0, binding = 0) readonly buffer sb {
	int[] quads;
};

layout(location = 0) out flat ivec2 corner;
layout(location = 1) out flat ivec2 size;
layout(location = 2) out flat int bufferIndex;
layout(location = 3) out flat int sectionWidth;
layout(location = 4) out flat int scale;
layout(location = 5) out flat int rawColor;

void main() {
	int quadIndex = 8 * (gl_VertexIndex / 6);
	int vertexIndex = gl_VertexIndex % 6;

	corner = ivec2(quads[quadIndex], quads[quadIndex + 1]);
	size = ivec2(quads[quadIndex + 2], quads[quadIndex + 3]);
	bufferIndex = quads[quadIndex + 4];
	sectionWidth = quads[quadIndex + 5];
	scale = quads[quadIndex + 6];
	rawColor = quads[quadIndex + 7];

	int x = corner.x;
	int y = corner.y;
	if (vertexIndex >= 1 && vertexIndex <= 3) x += size.x;
	if (vertexIndex >= 2 && vertexIndex <= 4) y += size.y;
	gl_Position = vec4(2.0 * float(x) / framebufferWidth - 1.0, 2.0 * float(y) / framebufferHeight - 1.0, 0.0, 1.0);
}
