float determineStrokeIntensity(float distance, float strokeWidth) {
	// DESIRED BEHAVIOR:
	// When strokeWidth = 1.0:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 1.0 & 0.0
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.0 & 0.9 & 0.1
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.3 & 0.7 & 0.0
	// formula: alpha = 1.0 - distance

	// When strokeWidth = 0.5:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 0.5 & 0.0
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.0 & 0.45 & 0.05
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.15 & 0.35 & 0.0
	// formula: alpha = 0.5 - distance * 0.5

	// When strokeWidth = 0.1:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 0.1 & 0.0
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.0 & 0.09 & 0.01
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.07 & 0.03 & 0.0
	// formula: alpha = 0.1 - distance * 0.1

	// When strokeWidth = 2.0:
	// - distance = 1.0 & 0.0 & 1.0 -> alpha = 0.5 & 1.0 & 0.5
	// - distance = 1.1 & 0.1 & 0.9 -> alpha = 0.4 & 1.0 & 0.6
	// - distance = 0.7 & 0.3 & 1.3 -> alpha = 0.8 & 1.0 & 0.2
	// - distance = 1.5 & 0.5 & 0.5 -> alpha = 0.0 & 1.0 & 1.0
	// formula: alpha = 1.5 - distance

	// When strokeWidth = 3.0:
	// - distance = 2.0 & 1.0 & 0.0 & 1.0 -> alpha = 0.0 & 1.0 & 1.0 & 1.0
	// - distance = 1.1 & 0.1 & 0.9 & 1.9 -> alpha = 0.9 & 1.0 & 1.0 & 0.1
	// - distance = 1.7 & 0.7 & 0.3 & 1.3 -> alpha = 0.3 & 1.0 & 1.0 & 0.7
	// formula: alpha = 2.0 - distance

	if (strokeWidth >= 1.0) return clamp(0.5 + 0.5 * strokeWidth - distance, 0.0, 1.0);
	else return clamp(strokeWidth - strokeWidth * distance, 0.0, 1.0);
}

vec4 determineMainColor(bool inside, float distance, vec4 fillColor) {
	float clampedDistance = clamp(distance, 0.0, 0.5);

	// The desired behavior is:
	// - inside && distance == 0 -> 0.5 * fillColor
	// - outside && distance == 0 -> 0.5 * fillColor
	// - inside && distance == 0.25 -> 0.75 * fillColor
	// - outside && distance == 0.25 -> 0.25 * fillColor
	// - inside && distance >= 0.5 -> 1.0 * fillColor
	// - outside && distance >= 0.5 -> 0.0 * fillColor
	if (inside) return (0.5 + clampedDistance) * fillColor;
	else return (0.5 - clampedDistance) * fillColor;
}

vec4 mixStrokeColor(vec4 mainColor, vec4 strokeColor, float strokeIntensity) {
	float strokeAlpha = strokeColor.a * strokeIntensity;
	return (1.0 - strokeAlpha) * mainColor + vec4(strokeAlpha * strokeColor.rgb, strokeAlpha);
}
