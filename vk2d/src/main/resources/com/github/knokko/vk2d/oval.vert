#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 center;
layout(location = 2) in vec2 radius;
layout(location = 3) in uint centerColor;
layout(location = 4) in uvec4 colors;
layout(location = 5) in vec4 colorDistances;

layout(location = 0) out vec2 propagatePosition;
layout(location = 1) out vec2 propagateCenter;
layout(location = 2) out vec2 propagateRadius;
layout(location = 3) out flat uint propagateCenterColor;
layout(location = 4) out flat uvec4 propagateColors;
layout(location = 5) out vec4 propagateColorDistances;

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagatePosition = position;
	propagateCenter = center;
	propagateRadius = radius;
	propagateCenterColor = centerColor;
	propagateColors = colors;
	propagateColorDistances = colorDistances;
}
