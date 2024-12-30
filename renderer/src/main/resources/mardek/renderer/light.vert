#version 450

layout(location = 0) in ivec2 offset;
layout(location = 1) in uint radius;
layout(location = 2) in uint color;

layout(push_constant) uniform PushConstants {
	uvec2 viewportSize;
};

layout(location = 0) out vec2 relativeCoordinates;
layout(location = 1) out uint propagateColor;

vec2 relativeCoordinateMapping[] = {
	vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
	vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0)
};

void main() {
	relativeCoordinates = relativeCoordinateMapping[gl_VertexIndex];
	vec2 relative = vec2(offset + 2 * radius * relativeCoordinates) / vec2(viewportSize);
	gl_Position = vec4(2.0 * relative - vec2(1.0), 0.0, 1.0);
	propagateColor = color;
}
