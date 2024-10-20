#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat int textureIndex;

layout(set = 0, binding = 0) uniform sampler entitySampler;
layout(set = 0, binding = 1) uniform texture2DArray entityImages;

layout(location = 0) out vec4 outColor;

void main() {
	outColor = texture(sampler2DArray(entityImages, entitySampler), vec3(textureCoordinates, textureIndex));
}
