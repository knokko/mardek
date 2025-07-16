#version 450

// Based on: http://wdobbie.com/post/gpu-text-rendering-with-vector-textures/

struct Curve {
	vec2 p0, p1, p2;
};

layout(set = 0, binding = 0) readonly buffer CurveBuffer {
	uint curveData[];
};

layout(location = 0) in vec2 uv;
layout(location = 1) in flat uint firstCurve;
layout(location = 2) in flat uint numCurves;
layout(location = 3) in vec4 color;

layout(location = 0) out vec4 outColor;

float transformCoordinate(uint raw) {
	return raw * 2.5 / 1023.0 - 0.5;
}

Curve loadCurve(uint index) {
	Curve result;
	uint packedX = curveData[2 * index];
	uint packedY = curveData[2 * index + 1];
	result.p0.x = transformCoordinate(packedX & 1023);
	result.p0.y = transformCoordinate(packedY & 1023);
	result.p1.x = transformCoordinate((packedX >> 10) & 1023);
	result.p1.y = transformCoordinate((packedY >> 10) & 1023);
	result.p2.x = transformCoordinate((packedX >> 20) & 1023);
    result.p2.y = transformCoordinate((packedY >> 20) & 1023);
	return result;
}

float computeCoverage(float inverseDiameter, vec2 p0, vec2 p1, vec2 p2) {
	if (p0.y > 0 && p1.y > 0 && p2.y > 0) return 0.0;
	if (p0.y < 0 && p1.y < 0 && p2.y < 0) return 0.0;

	// Note: Simplified from abc formula by extracting a factor of (-2) from b.
	vec2 a = p0 - 2*p1 + p2;
	vec2 b = p0 - p1;
	vec2 c = p0;

	float t0, t1;
	if (abs(a.y) >= 1e-5) {
		// Quadratic segment, solve abc formula to find roots.
		float radicand = b.y*b.y - a.y*c.y;
		if (radicand <= 0) return 0.0;

		float s = sqrt(radicand);
		t0 = (b.y - s) / a.y;
		t1 = (b.y + s) / a.y;
	} else {
		// Linear segment, avoid division by a.y, which is near zero.
		// There is only one root, so we have to decide which variable to
		// assign it to based on the direction of the segment, to ensure that
		// the ray always exits the shape at t0 and enters at t1. For a
		// quadratic segment this works 'automatically', see readme.
		float t = p0.y / (p0.y - p2.y);
		if (p0.y < p2.y) {
			t0 = -1.0;
			t1 = t;
		} else {
			t0 = t;
			t1 = -1.0;
		}
	}

	float alpha = 0;

	if (t0 >= 0 && t0 < 1) {
		float x = (a.x*t0 - 2.0*b.x)*t0 + c.x;
		alpha += clamp(x * inverseDiameter + 0.5, 0, 1);
	}

	if (t1 >= 0 && t1 < 1) {
		float x = (a.x*t1 - 2.0*b.x)*t1 + c.x;
		alpha -= clamp(x * inverseDiameter + 0.5, 0, 1);
	}

	return alpha;
}

vec2 rotate(vec2 v) {
	return vec2(v.y, -v.x);
}

void main() {
	bool enableSuperSamplingAntiAliasing = true;
	float alpha = 0;

	// Inverse of the diameter of a pixel in uv units for anti-aliasing.
	float antiAliasingWindowSize = 1.0;
	vec2 inverseDiameter = 1.0 / (antiAliasingWindowSize * fwidth(uv));

	for (uint i = 0; i < numCurves; i++) {
		Curve curve = loadCurve(firstCurve + i);

		vec2 p0 = curve.p0 - uv;
		vec2 p1 = curve.p1 - uv;
		vec2 p2 = curve.p2 - uv;

		alpha += computeCoverage(inverseDiameter.x, p0, p1, p2);
		if (enableSuperSamplingAntiAliasing) {
			alpha += computeCoverage(inverseDiameter.y, rotate(p0), rotate(p1), rotate(p2));
		}
	}

	if (enableSuperSamplingAntiAliasing) {
		alpha *= 0.5;
	}

	alpha = clamp(alpha, 0.0, 1.0);
	outColor = color * alpha;
}
