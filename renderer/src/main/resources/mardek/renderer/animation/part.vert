#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 mainTextureCoordinates;
layout(location = 2) in vec2 maskTextureCoordinates;
layout(location = 3) in uvec3 colorTransform;

layout(location = 0) out vec2 propagateMainTextureCoordinates;
layout(location = 1) out vec2 propagateMaskTextureCoordinates;
layout(location = 2) out vec4 addColor;
layout(location = 3) out vec4 multiplyColor;
layout(location = 4) out vec4 subtractColor;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	propagateMainTextureCoordinates = mainTextureCoordinates;
	propagateMaskTextureCoordinates = maskTextureCoordinates;
	addColor = decodeColor(colorTransform.x);
	multiplyColor = decodeColor(colorTransform.y);
	subtractColor = decodeColor(colorTransform.z);
}
