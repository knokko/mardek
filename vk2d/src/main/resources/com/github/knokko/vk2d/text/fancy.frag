#version 450

#include "fancy-style.glsl"

layout(location = 0) in vec4 vertexInfo;
layout(location = 1) in flat FancyTextStyle style;

layout(set = 2, binding = 0) uniform sampler2D textureSampler;

layout(push_constant) uniform PushConstants {
	layout(offset = 16) float atlasHeightA;
	float distanceFactor;
};

layout(location = 0) out vec4 outColor;

vec4 blendColors(vec4 back, vec4 front) {
	float alpha = front.a + back.a * (1.0 - front.a);
	vec3 color = (front.a * front.rgb + back.a * (1.0 - front.a) * back.rgb) / alpha;
	return vec4(color, alpha);
}

void main() {
	vec2 textureCoordinates = vertexInfo.xy;
	float heightA = vertexInfo.z;
	float relativeY = vertexInfo.w;
	float rawSignedDistance = texture(textureSampler, textureCoordinates).r;
	float signedDistance = rawSignedDistance * heightA / atlasHeightA / distanceFactor;
	signedDistance = signedDistance * getDistanceFactor(style) + getDistanceBias(style) * heightA;

	vec4 fillColor = getFillColor(style, relativeY);
	fillColor.a *= 0.5 + clamp(signedDistance, -0.5, 0.5);

	vec4 strokeColor = getStrokeColor(style, signedDistance / heightA);

	if (shouldStrokeBehindFill(style)) outColor = blendColors(strokeColor, fillColor);
	else outColor = blendColors(fillColor, strokeColor);
}
