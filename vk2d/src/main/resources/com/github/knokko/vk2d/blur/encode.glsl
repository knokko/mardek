uint encodeColorComponent(float component) {
	return clamp(uint(255.0 * component + 0.5), 0, 255);
}

uint encodeColor(vec4 color) {
	uint red = encodeColorComponent(color.r);
	uint green = encodeColorComponent(color.g);
	uint blue = encodeColorComponent(color.b);
	uint alpha = encodeColorComponent(color.a);
	return red | (green << 8) | (blue << 16) | (alpha << 24);
}
