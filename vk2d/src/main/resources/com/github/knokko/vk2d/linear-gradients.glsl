struct SelectedGradient {
	uint color0, color1;
	float distance0, distance1;
};

SelectedGradient initialSelectedGradient(uint centerColor, uint color0, float distance0) {
	SelectedGradient initial;
	initial.color0 = centerColor;
	initial.color1 = color0;
	initial.distance0 = 0.0;
	initial.distance1 = distance0;
	return initial;
}

SelectedGradient nextSelectedGradient(
	float distance, SelectedGradient current, uint color0, uint color1, float distance0, float distance1
) {
	if (distance < distance0) return current;
	current.color0 = color0;
	current.color1 = color1;
	current.distance0 = distance0;
	current.distance1 = distance1;
	return current;
}

vec4 selectGradientColor(float distance, SelectedGradient selected) {
	if (distance >= selected.distance1 || selected.color0 == selected.color1) {
		return decodeColor(selected.color1);
	}

	float slider = (distance - selected.distance0) / (selected.distance1 - selected.distance0);
	return slider * decodeColor(selected.color1) + (1.0 - slider) * decodeColor(selected.color0);
}

vec4 computeGradientColor(float distance, uint baseColor, vec4 distances, uvec4 colors) {
	if (distances.x > 100.0 * distance) return decodeColor(baseColor);
	SelectedGradient selected = initialSelectedGradient(baseColor, colors.x, distances.x);
	selected = nextSelectedGradient(distance, selected, colors.x, colors.y, distances.x, distances.y);
	selected = nextSelectedGradient(distance, selected, colors.y, colors.z, distances.y, distances.z);
	selected = nextSelectedGradient(distance, selected, colors.z, colors.w, distances.z, distances.w);
	return selectGradientColor(distance, selected);
}
