vec4 decodeColor(uint color) {
	uint red = color & 255u;
	uint green = (color >> 8) & 255u;
	uint blue = (color >> 16) & 255u;
	uint alpha = (color >> 24) & 255u;
	return vec4(red / 255.0, green / 255.0, blue / 255.0, alpha / 255.0);
}

ivec2 decodePosition(uint position) {
	uint rawX = position & 0xFFFF;
	uint rawY = (position >> 16) & 0xFFFF;
	return ivec2(int(rawX) - 10000, (int(rawY) - 10000));
}

vec2 decodePosition(uint position, uvec2 viewportSize) {
	return 2.0 * decodePosition(position) / viewportSize - vec2(1.0);
}
