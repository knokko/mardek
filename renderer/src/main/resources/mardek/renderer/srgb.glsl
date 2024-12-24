float srgbToLinear(float srgb) {
	if (srgb <= 0.04) return srgb / 12.92;
	else return pow((srgb + 0.055) / 1.055, 2.4);
}

vec3 srgbToLinear(vec3 srgb) {
	return vec3(srgbToLinear(srgb.r), srgbToLinear(srgb.g), srgbToLinear(srgb.b));
}
