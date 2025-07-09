#version 450

layout(location = 0) in vec2 textureCoordinates;
layout(location = 1) in flat uint textureIndex;
layout(location = 2) in flat uint header;
layout(location = 3) in flat uvec4 firstColors;
layout(location = 4) in flat uint rawBlinkColor;
layout(location = 5) in float opacity;

layout(set = 0, binding = 0) readonly buffer TextureData {
	uint textureData[];
};

layout(location = 0) out vec4 outColor;

#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/decode.glsl"
#include "../../../../../../../vk2d/src/main/resources/com/github/knokko/vk2d/kim3.glsl"

defineSampleKim3(textureData)

void main() {
	vec4 textureColor = sampleKim3(header, textureIndex, firstColors, textureCoordinates);
	float alpha = textureColor.a * opacity;
	vec4 blinkColor = decodeColor(rawBlinkColor);
	vec3 rgbColor = (1.0 - blinkColor.a) * textureColor.rgb + blinkColor.a * blinkColor.rgb;
	outColor = vec4(rgbColor, alpha);
}
