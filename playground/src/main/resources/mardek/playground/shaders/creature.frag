#version 450
#extension GL_EXT_nonuniform_qualifier : enable

layout(location = 0) in flat int textureIndex;
layout(location = 1) in vec2 textureCoordinates;

layout(set = 0, binding = 0) uniform texture2D partImages[50];
layout(set = 0, binding = 1) uniform sampler partSampler;
layout(set = 0, binding = 2) readonly buffer ColorBuffer {
	int colorBuffer[50 * 2]; // TODO Spec constant
};

layout(location = 0) out vec4 outColor;

vec4 decodeColor(int rgba) {
	vec4 color;
	color.r = (rgba & 255) / 255.0;
	color.g = ((rgba >> 8) & 255) / 255.0;
	color.b = ((rgba >> 16) & 255) / 255.0;
	color.a = ((rgba >> 24) & 255) / 255.0;
	return color;
}

void main() {
	vec4 multColor = decodeColor(colorBuffer[2 * textureIndex]);
	vec4 addColor = decodeColor(colorBuffer[2 * textureIndex + 1]);
	outColor = texture(sampler2D(partImages[nonuniformEXT(textureIndex)], partSampler), textureCoordinates) * multColor + addColor;
}
