#version 450

layout(location = 0) in uvec2 colorTransform;

layout(push_constant) uniform PushConstants {
	uvec2 textureSize;
	vec2 minPosition;
	vec2 boundPosition;
};

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out vec4 addColor;
layout(location = 2) out vec4 multiplyColor;

vec2 textureCoordinateMapping[] = {
	vec2(0.0), vec2(0.0, 1.0), vec2(1.0),
	vec2(1.0), vec2(1.0, 0.0), vec2(0.0)
};

#include "../decode.glsl"

void main() {
	textureCoordinates = textureCoordinateMapping[gl_VertexIndex % 6];
	gl_Position = vec4(minPosition + textureCoordinates * (boundPosition - minPosition), 0.0, 1.0);
	addColor = decodeColor(colorTransform.x);
	multiplyColor = decodeColor(colorTransform.y);
}
