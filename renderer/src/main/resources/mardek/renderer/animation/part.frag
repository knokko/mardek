#version 450

layout(location = 0) in vec2 mainTextureCoordinates;
layout(location = 1) in vec2 maskTextureCoordinates;
layout(location = 2) in vec4 addColor;
layout(location = 3) in vec4 multiplyColor;
layout(location = 4) in vec4 subtractColor;

layout(set = 0, binding = 0) uniform sampler2D textureSampler;
layout(set = 1, binding = 0) uniform sampler2D maskSampler;

layout(location = 0) out vec4 outColor;

void main() {
	vec4 textureColor = texture(textureSampler, mainTextureCoordinates);
	outColor = addColor - subtractColor + multiplyColor * textureColor;

    if (maskTextureCoordinates.x >= 0.0 && maskTextureCoordinates.x <= 1.0 && maskTextureCoordinates.y >= 0.0 && maskTextureCoordinates.y <= 1.0) {
        vec4 maskColor = texture(maskSampler, maskTextureCoordinates);
        outColor.a *= maskColor.r;
    } else outColor.a = 0.0;
}
