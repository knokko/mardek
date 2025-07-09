#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoordinates;
layout(location = 2) in uvec2 colorTransform;

layout(location = 0) out vec2 propagateTextureCoordinates;
layout(location = 1) out vec4 addColor;
layout(location = 2) out vec4 multiplyColor;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagateTextureCoordinates = textureCoordinates;
	addColor = decodeColor(colorTransform.x);
	multiplyColor = decodeColor(colorTransform.y);
}
