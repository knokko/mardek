#version 450

layout(location = 0) in ivec2 offset;
layout(location = 1) in uvec2 spriteSize;
layout(location = 2) in float scale;
layout(location = 3) in int spriteOffset;
layout(location = 4) in float opacity;
layout(location = 5) in float rotation;
layout(location = 6) in int blinkColor;
layout(location = 7) in float blinkIntensity;

layout(push_constant) uniform PushConstants {
	ivec2 viewportSize;
};

layout(location = 0) out vec2 textureCoordinates;
layout(location = 1) out uvec2 propagateSpriteSize;
layout(location = 2) out int propagateSpriteOffset;
layout(location = 3) out float propagateOpacity;
layout(location = 4) out int propagateBlinkColor;
layout(location = 5) out float propagateBlinkIntensity;

vec2 textureCoordinateMapping[] = {
		vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0),
		vec2(1.0, 1.0), vec2(0.0, 1.0), vec2(0.0, 0.0)
};

void main() {
	textureCoordinates = textureCoordinateMapping[gl_VertexIndex];
	vec2 diagonal = scale * spriteSize;
	vec2 middle = vec2(offset + diagonal * 0.5);

	mat2 rotationMatrix = mat2(
		cos(rotation), -sin(rotation),
		sin(rotation), cos(rotation)
	);

	vec2 relative = vec2(middle + rotationMatrix * (textureCoordinates - vec2(0.5)) * diagonal) / vec2(viewportSize);
	gl_Position = vec4(2.0 * relative - vec2(1.0), 0.0, 1.0);
	propagateSpriteSize = spriteSize;
	propagateSpriteOffset = spriteOffset;
	propagateOpacity = opacity;
	propagateBlinkColor = blinkColor;
	propagateBlinkIntensity = blinkIntensity;
}
