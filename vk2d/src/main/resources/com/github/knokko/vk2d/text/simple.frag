#version 450

#include "simple-style.glsl"

layout(location = 0) in vec3 textureCoordinatesAndHeightA;
layout(location = 1) in flat TextStyle style;

layout(set = 2, binding = 0) uniform sampler2D textureSampler;

layout(push_constant) uniform PushConstants {
	vec2 atlasSize;
	float atlasHeightA;
	float distanceFactor;
};

layout(location = 0) out vec4 outColor;

vec4 blendColors(vec4 back, vec4 front) {
	float alpha = front.a + back.a * (1.0 - front.a);
	vec3 color = (front.a * front.rgb + back.a * (1.0 - front.a) * back.rgb) / alpha;
	return vec4(color, alpha);
}

void main() {
	float heightA = textureCoordinatesAndHeightA.z;
	float rawSignedDistance = texture(textureSampler, textureCoordinatesAndHeightA.xy).r;
	float signedDistance = rawSignedDistance * heightA / atlasHeightA / distanceFactor;
	signedDistance = signedDistance * getDistanceFactor(style) + getDistanceBias(style);

	vec4 fillColor = getFillColor(style);
	fillColor.a *= 0.5 + clamp(signedDistance, -0.5, 0.5);

	bool strokeBehindFill = isStrokeBehindFillColor(style);
	float strokeWidth = getStrokeWidth(style) * heightA;
	if (strokeWidth > 0.01) {
		vec4 strokeColor = getStrokeColor(style);
    	strokeColor.a *= pow(1.0 - min(abs(signedDistance / strokeWidth), 1.0), getStrokePower(style));

		if (strokeBehindFill) outColor = blendColors(strokeColor, fillColor);
		else outColor = blendColors(fillColor, strokeColor);
	} else {
		outColor = fillColor;
	}
}
