#version 450

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
	uvec2 renderOffset;
	uvec2 tableSize;
	uint tableOffset;
	uint table;
	uint scale;
};

layout(location = 0) out vec2 textureCoordinates;

vec2 textureCoordinateMapping[] = {
	vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
	vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0)
};

void main() {
	textureCoordinates = textureCoordinateMapping[gl_VertexIndex];
	vec2 relative = vec2(renderOffset + scale * tableSize * textureCoordinates) / vec2(viewportSize);
	gl_Position = vec4(2.0 * relative - vec2(1.0), 0.0, 1.0);
}
