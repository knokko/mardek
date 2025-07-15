#version 450

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 uv;
layout(location = 2) in uint firstCurve;
layout(location = 3) in uint numCurves;
layout(location = 4) in uint rawColor;

layout(location = 0) out vec2 propagateUV;
layout(location = 1) out flat uint propagateFirstCurve;
layout(location = 2) out flat uint propagateNumCurves;
layout(location = 3) out vec4 color;

#include "decode.glsl"

void main() {
	gl_Position = vec4(position, 0, 1);
	propagateUV = uv;
	propagateFirstCurve = firstCurve;
	propagateNumCurves = numCurves;
	color = decodeColor(rawColor);
}
