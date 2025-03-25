#version 450

layout(push_constant) uniform pc {
    int quadBufferOffset;
	int framebufferWidth;
	int framebufferHeight;
};
layout(set = 0, binding = 0) readonly buffer sb {
	int[] quads;
};
layout(set = 0, binding = 2) readonly buffer sb2 {
	int[] extra;
};

layout(location = 0) out flat ivec2 corner;
layout(location = 1) out flat ivec2 size;
layout(location = 2) out flat int type;
layout(location = 3) out flat int extraIndex;

layout(location = 4) out flat int bufferIndex;
layout(location = 5) out flat int sectionWidth;
layout(location = 6) out flat int scale;
layout(location = 7) out flat int colorIndex;
layout(location = 8) out flat int rawColor;
layout(location = 9) out flat int outlineWidth;

void main() {
	int quadIndex = quadBufferOffset + 6 * (gl_VertexIndex / 6);
	int rawVertexIndex = gl_VertexIndex % 6;
	int vertexIndex = rawVertexIndex;
	if (rawVertexIndex == 3) vertexIndex = 2;
	if (rawVertexIndex == 4) vertexIndex = 3;
	if (rawVertexIndex == 5) vertexIndex = 0;

	corner = ivec2(quads[quadIndex], quads[quadIndex + 1]);
	size = ivec2(quads[quadIndex + 2], quads[quadIndex + 3]);
	type = quads[quadIndex + 4];
	extraIndex = quads[quadIndex + 5];

    int x = corner.x;
	int y = corner.y;
	if (vertexIndex == 1 || vertexIndex == 2) x += size.x;
	if (vertexIndex >= 2) y += size.y;

	if (type > 1000) {
		type -= 1000;
		x = extra[extraIndex + 2 * vertexIndex];
		y = extra[extraIndex + 2 * vertexIndex + 1];
		extraIndex += 8;
    }

	if (type == 2) {
		bufferIndex = extra[extraIndex];
		sectionWidth = extra[extraIndex + 1];
		scale = extra[extraIndex + 2];
		colorIndex = extra[extraIndex + 3];
		rawColor = extra[colorIndex];
		outlineWidth = extra[colorIndex + 1];
	}

	gl_Position = vec4(2.0 * float(x) / framebufferWidth - 1.0, 2.0 * float(y) / framebufferHeight - 1.0, 0.0, 1.0);
}
