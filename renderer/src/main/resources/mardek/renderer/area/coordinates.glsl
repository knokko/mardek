vec2 deriveTextureCoordinates(ivec2 basePosition, ivec2 size, ivec2 minPosition, ivec2 boundPosition) {
	uint rawVertex = gl_VertexIndex % 6;
	ivec2 offset = ivec2(0);
	if (rawVertex >= 1 && rawVertex <= 3) offset.x = size.x;
	if (rawVertex >= 2 && rawVertex <= 4) offset.y = size.y;

	ivec2 desiredPosition = basePosition + offset;
	ivec2 clampedPosition = clamp(desiredPosition, minPosition, boundPosition);
	gl_Position = vec4(2.0 * clampedPosition / viewportSize - vec2(1.0), 0.0, 1.0);

	float minU = max(0.0, float(clampedPosition.x - desiredPosition.x) / size.x);
	float minV = max(0.0, float(clampedPosition.y - desiredPosition.y) / size.y);
	float maxU = min(1.0, 1.0 - float(desiredPosition.x - clampedPosition.x) / size.x);
	float maxV = min(1.0, 1.0 - float(desiredPosition.y - clampedPosition.y) / size.y);

	vec2 textureCoordinates;
	textureCoordinates.x = rawVertex >= 1 && rawVertex <= 3 ? maxU : minU;
	textureCoordinates.y = rawVertex >= 2 && rawVertex <= 4 ? maxV : minV;
	return textureCoordinates;
}
