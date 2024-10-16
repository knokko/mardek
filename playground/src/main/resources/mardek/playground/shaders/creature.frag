#version 450
#extension GL_EXT_nonuniform_qualifier : enable

layout(location = 0) in flat int textureIndex;
layout(location = 1) in vec2 textureCoordinates;

layout(set = 0, binding = 0) uniform texture2D partImages[50];
layout(set = 0, binding = 1) uniform sampler partSampler;

layout(location = 0) out vec4 outColor;

void main() {
	outColor = texture(sampler2D(partImages[nonuniformEXT(textureIndex)], partSampler), textureCoordinates);
}
