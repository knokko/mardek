vec4 decodeColor(uint color) {
	uint red = color & 255u;
	uint green = (color >> 8) & 255u;
	uint blue = (color >> 16) & 255u;
	uint alpha = (color >> 24) & 255u;
	return vec4(red / 255.0, green / 255.0, blue / 255.0, alpha / 255.0);
}
